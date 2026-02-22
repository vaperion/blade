package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;

public class DoubleArgument extends AbstractNumericArgument<Double> {
    @Override
    public @Nullable Double provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        double input;
        try {
            input = Double.parseDouble(arg.requireValue());
        } catch (NumberFormatException e) {
            throw BladeParseError.fatal(String.format(
                "'%s' is not a valid decimal number.",
                arg.value()
            ));
        }

        validateRange(arg, input);

        return input;
    }

    @Override
    public @Nullable String defaultArgName(@NotNull AnnotatedElement element) {
        return "decimal";
    }
}
