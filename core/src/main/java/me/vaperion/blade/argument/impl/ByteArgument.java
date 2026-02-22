package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ByteArgument extends AbstractNumericArgument<Byte> {
    @Override
    public @Nullable Byte provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        byte input;
        try {
            input = Byte.parseByte(arg.requireValue());
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
