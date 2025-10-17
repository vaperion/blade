package me.vaperion.blade.argument.impl;

import me.vaperion.blade.annotation.parameter.Range;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

public class IntArgument implements ArgumentProvider<Integer> {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.#");

    @Override
    public @Nullable Integer provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        int input;
        try {
            input = Integer.parseInt(arg.requireValue());
        } catch (NumberFormatException e) {
            throw BladeParseError.fatal(String.format(
                "'%s' is not a valid whole number.",
                arg.value()
            ));
        }

        if (arg.parameter().hasRange()) {
            Range range = arg.parameter().range();
            assert range != null;

            if (!Double.isNaN(range.min()) && input < range.min()) {
                throw BladeParseError.fatal(String.format(
                    "Number must be at least %s (you entered %s).",
                    NUMBER_FORMAT.format(range.min()),
                    NUMBER_FORMAT.format(input)
                ));
            }

            if (!Double.isNaN(range.max()) && input > range.max()) {
                throw BladeParseError.fatal(String.format(
                    "Number must be at most %s (you entered %s).",
                    NUMBER_FORMAT.format(range.max()),
                    NUMBER_FORMAT.format(input)
                ));
            }
        }

        return input;
    }
}
