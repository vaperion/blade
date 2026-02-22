package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;

public class FloatArgument extends AbstractNumericArgument<Float> {
    @Override
    public @Nullable Float provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        float input;
        try {
            input = Float.parseFloat(arg.requireValue());
        } catch (NumberFormatException e) {
            throw BladeParseError.fatal(String.format(
                "Invalid floating-point number: `%s`.",
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
