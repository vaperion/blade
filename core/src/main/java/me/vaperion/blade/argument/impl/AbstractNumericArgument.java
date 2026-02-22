package me.vaperion.blade.argument.impl;

import me.vaperion.blade.annotation.parameter.Range;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.exception.BladeParseError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.text.DecimalFormat;

abstract class AbstractNumericArgument<T extends Number> implements ArgumentProvider<T> {

    private static final ThreadLocal<DecimalFormat> NUMBER_FORMAT =
        ThreadLocal.withInitial(() -> new DecimalFormat("#.#"));

    @Override
    public @Nullable String defaultArgName(@NotNull AnnotatedElement element) {
        return "number";
    }

    protected final void validateRange(@NotNull InputArgument arg, double input) {
        if (!arg.parameter().hasRange()) {
            return;
        }

        Range range = arg.parameter().range();
        assert range != null;

        if (!Double.isNaN(range.min()) && input < range.min()) {
            throw BladeParseError.fatal(String.format(
                "Number must be at least %s (you entered %s).",
                formatNumber(range.min()),
                formatNumber(input)
            ));
        }

        if (!Double.isNaN(range.max()) && input > range.max()) {
            throw BladeParseError.fatal(String.format(
                "Number must be at most %s (you entered %s).",
                formatNumber(range.max()),
                formatNumber(input)
            ));
        }
    }

    @NotNull
    private String formatNumber(double value) {
        return NUMBER_FORMAT.get().format(value);
    }
}
