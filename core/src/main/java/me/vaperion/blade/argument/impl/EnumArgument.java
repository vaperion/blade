package me.vaperion.blade.argument.impl;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.vaperion.blade.annotation.api.HiddenEnumValue;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({ "rawtypes" })
public class EnumArgument implements ArgumentProvider<Enum> {

    private final Map<Class<?>, EnumContainer> container = new IdentityHashMap<>();

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
                .filter(wrapped -> !wrapped.hidden)
                .map(wrapped -> wrapped.value.name())
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

        String joinedNames = matches.stream()
            .filter(wrapped -> !wrapped.hidden)
            .map(wrapped -> wrapped.value.name())
            .collect(Collectors.joining(", "));

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
            if (wrapped.hidden) continue;

            String lowerName = wrapped.value.name().toLowerCase(Locale.ROOT);

            if (lowerName.startsWith(input))
                suggestions.suggest(wrapped.value.name());
        }
    }

    @NotNull
    private List<WrappedEnum> match(@NotNull EnumContainer container,
                                    @NotNull String input) {
        input = input.toLowerCase(Locale.ROOT);

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

        List<WrappedEnum> matches = new ArrayList<>();

        for (WrappedEnum wrapped : container.constants) {
            if (wrapped.blocked) continue;

            String lowerName = wrapped.value.name().toLowerCase(Locale.ROOT);

            if (lowerName.startsWith(input)) {
                matches.add(wrapped);
            }
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
                    HiddenEnumValue annotation = clazz.getField(value.name()).getAnnotation(HiddenEnumValue.class);

                    boolean hidden = annotation != null;
                    boolean blocked = hidden && annotation.block();

                    this.constants.add(new WrappedEnum(value, hidden, blocked));
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Unexpected error accessing enum field", e);
                }
            }
        }
    }

    @RequiredArgsConstructor
    @ToString
    static class WrappedEnum {
        private final Enum value;
        private final boolean hidden, blocked;
    }
}
