package me.vaperion.blade.argument.impl;

import lombok.ToString;
import me.vaperion.blade.annotation.api.EnumValue;
import me.vaperion.blade.annotation.api.HiddenEnumValue;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings({ "rawtypes", "deprecation" })
public class EnumArgument implements ArgumentProvider<Enum> {

    private final Map<Class<?>, EnumContainer> container = new ConcurrentHashMap<>();

    @NotNull
    private EnumContainer container(@NotNull Class<?> type) {
        return container.computeIfAbsent(type, EnumContainer::new);
    }

    @Override
    public @Nullable Enum provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        EnumContainer container = container(arg.parameter().type());

        List<WrappedEnum> matches = match(container, arg.requireValue());

        if (matches.isEmpty()) {
            String joinedNames = container.constants.stream()
                .filter(wrapped -> !wrapped.hidden && !wrapped.blocked)
                .map(wrapped -> wrapped.displayName)
                .collect(Collectors.joining(", "));

            throw BladeParseError.recoverable(String.format(
                "'%s' is not a valid option. Use: %s",
                arg.value(),
                joinedNames
            ));
        }

        if (matches.size() == 1) {
            return matches.get(0).value;
        }

        int highestPriority = matches.stream()
            .mapToInt(wrapped -> wrapped.priority)
            .max()
            .orElse(0);

        List<WrappedEnum> prioritizedMatches = matches.stream()
            .filter(wrapped -> wrapped.priority == highestPriority)
            .collect(Collectors.toList());

        if (prioritizedMatches.size() == 1) {
            return prioritizedMatches.get(0).value;
        }

        String joinedNames = prioritizedMatches.stream()
            .filter(wrapped -> !wrapped.hidden && !wrapped.blocked)
            .map(wrapped -> wrapped.displayName)
            .collect(Collectors.joining(", "));

        if (joinedNames.isEmpty()) {
            throw BladeParseError.recoverable(String.format(
                "'%s' is ambiguous. Please provide a more specific value.",
                arg.value()
            ));
        }

        throw BladeParseError.recoverable(String.format(
            "'%s' is ambiguous. Did you mean: %s?",
            arg.value(),
            joinedNames
        ));
    }

    @Override
    public void suggest(@NotNull Context ctx,
                        @NotNull InputArgument arg,
                        @NotNull SuggestionsBuilder suggestions) throws BladeParseError {
        EnumContainer container = container(arg.parameter().type());

        String input = arg.requireValue().toLowerCase(Locale.ROOT);

        for (WrappedEnum wrapped : container.constants) {
            if (wrapped.hidden || wrapped.blocked) continue;

            if (wrapped.startsWith(input)) {
                suggestions.suggest(wrapped.displayName);

                for (String alias : wrapped.aliases) {
                    if (alias.toLowerCase(Locale.ROOT).startsWith(input)) {
                        suggestions.suggest(alias);
                    }
                }
            }
        }
    }

    @Override
    public @Nullable String defaultArgName(@NotNull AnnotatedElement element) {
        return "enum";
    }

    @NotNull
    private List<WrappedEnum> match(@NotNull EnumContainer container,
                                    @NotNull String input) {
        input = input.toLowerCase(Locale.ROOT);

        List<WrappedEnum> matches = new ArrayList<>();

        for (WrappedEnum wrapped : container.constants) {
            if (wrapped.blocked) continue;
            if (wrapped.startsWith(input)) {
                matches.add(wrapped);
            }
        }

        if (!matches.isEmpty()) {
            return matches;
        }

        try {
            int intValue = Integer.parseInt(input);

            if (intValue < 0 || intValue >= container.constants.size()) {
                throw BladeParseError.recoverable(String.format(
                    "%d is not a valid index. Valid range: 0-%d",
                    intValue,
                    container.constants.size() - 1
                ));
            }

            WrappedEnum wrapped = container.constants.get(intValue);

            if (wrapped.blocked) {
                throw BladeParseError.recoverable(String.format(
                    "'%s' is not allowed.",
                    wrapped.value.name()
                ));
            }

            return Collections.singletonList(wrapped);
        } catch (NumberFormatException ignored) {
        }

        return matches;
    }

    @ToString
    static class EnumContainer {
        private final Class<?> clazz;
        private final List<WrappedEnum> constants;

        EnumContainer(@NotNull Class<?> clazz) {
            this.clazz = clazz;

            Enum[] values = (Enum[]) clazz.getEnumConstants();
            if (values == null)
                throw new IllegalArgumentException("Class " + clazz.getName() + " is not an enum type");

            this.constants = new ArrayList<>();

            for (Enum value : values) {
                try {
                    Field field = clazz.getField(value.name());

                    HiddenEnumValue hiddenValue = field.getAnnotation(HiddenEnumValue.class);
                    EnumValue enumValue = field.getAnnotation(EnumValue.class);

                    boolean hidden = hiddenValue != null || (enumValue != null && enumValue.hidden());
                    boolean blocked = (hiddenValue != null && hiddenValue.block())
                        || (enumValue != null && enumValue.block());

                    String displayName = value.name();
                    int priority = 0;
                    String[] aliases = new String[0];

                    if (enumValue != null) {
                        if (!enumValue.name().isEmpty()) {
                            displayName = enumValue.name();
                        }

                        aliases = enumValue.aliases().clone();
                        priority = enumValue.priority();
                    }

                    this.constants.add(new WrappedEnum(value, hidden, blocked, displayName, aliases, priority));
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Unexpected error accessing enum field", e);
                }
            }
        }
    }

    @ToString
    static class WrappedEnum {
        private final Enum value;
        private final boolean hidden, blocked;
        private final String displayName;
        private final String[] aliases;
        private final int priority;
        private final List<String> lowerCandidates;

        WrappedEnum(@NotNull Enum value,
                    boolean hidden,
                    boolean blocked,
                    @NotNull String displayName,
                    @NotNull String[] aliases,
                    int priority) {
            this.value = value;
            this.hidden = hidden;
            this.blocked = blocked;
            this.displayName = displayName;
            this.aliases = aliases;
            this.priority = priority;

            Set<String> lowerCandidates = new LinkedHashSet<>();
            lowerCandidates.add(value.name().toLowerCase(Locale.ROOT));
            lowerCandidates.add(displayName.toLowerCase(Locale.ROOT));
            for (String alias : aliases) {
                lowerCandidates.add(alias.toLowerCase(Locale.ROOT));
            }
            this.lowerCandidates = Collections.unmodifiableList(new ArrayList<>(lowerCandidates));
        }

        private boolean startsWith(@NotNull String input) {
            for (String candidate : lowerCandidates) {
                if (candidate.startsWith(input))
                    return true;
            }

            return false;
        }
    }
}
