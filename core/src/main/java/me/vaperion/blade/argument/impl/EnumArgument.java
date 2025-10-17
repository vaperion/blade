package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class EnumArgument implements ArgumentProvider<Enum> {

    private static Class<? extends Enum> enumClass;
    private static Enum[] enumConstants;

    private void load(@NotNull Class<?> type) {
        if (enumClass != null)
            return;

        enumClass = (Class<? extends Enum>) type;
        enumConstants = enumClass.getEnumConstants();
    }

    @Override
    public @Nullable Enum provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        load(arg.parameter().type());

        List<Enum> matches = match(arg.requireValue());

        if (matches.isEmpty()) {
            throw BladeParseError.recoverable(String.format(
                "'%s' is not a valid option. Valid options: %s",
                arg.value(),
                String.join(", ", Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .toArray(String[]::new)
                )
            ));
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }

        throw BladeParseError.recoverable(String.format(
            "'%s' is ambiguous. Did you mean: %s?",
            arg.value(),
            String.join(", ", matches.stream()
                .map(Enum::name)
                .toArray(String[]::new)
            )
        ));
    }

    @Override
    public void suggest(@NotNull Context ctx,
                        @NotNull InputArgument arg,
                        @NotNull SuggestionsBuilder suggestions) throws BladeParseError {
        enumClass = (Class<? extends Enum>) arg.parameter().type();

        String input = arg.requireValue().toLowerCase(Locale.ROOT);

        for (Enum value : enumClass.getEnumConstants()) {
            String name = value.name().toLowerCase(Locale.ROOT);

            if (name.startsWith(input))
                suggestions.suggest(name);
        }
    }

    @NotNull
    private List<Enum> match(@NotNull String input) {
        try {
            int intValue = Integer.parseInt(input);

            return Collections.singletonList(
                enumConstants[intValue]
            );
        } catch (Throwable ignored) {
        }

        List<Enum> matches = new ArrayList<>();

        for (Enum value : enumConstants) {
            if (value.name().toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))) {
                matches.add(value);
            }
        }

        return matches;
    }
}
