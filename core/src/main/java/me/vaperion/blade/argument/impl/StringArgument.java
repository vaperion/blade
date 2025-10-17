package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringArgument implements ArgumentProvider<String> {
    @Override
    public @Nullable String provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        if (arg.value() == null) {
            throw BladeParseError.recoverable("A text value is required.");
        }

        return arg.requireValue();
    }
}
