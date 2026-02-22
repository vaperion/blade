package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShortArgument extends AbstractNumericArgument<Short> {
    @Override
    public @Nullable Short provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        short input;
        try {
            input = Short.parseShort(arg.requireValue());
        } catch (NumberFormatException e) {
            throw BladeParseError.fatal(String.format(
                "'%s' is not a valid whole number.",
                arg.value()
            ));
        }

        validateRange(arg, input);

        return input;
    }
}
