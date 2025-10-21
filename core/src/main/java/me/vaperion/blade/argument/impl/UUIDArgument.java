package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.UUID;

public class UUIDArgument implements ArgumentProvider<UUID> {
    @Override
    public @Nullable UUID provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        try {
            return UUID.fromString(arg.requireValue());
        } catch (Throwable t) {
            throw BladeParseError.fatal(String.format(
                "'%s' is not a valid UUID.",
                arg.value()
            ));
        }
    }

    @Override
    public @Nullable String defaultArgName(@NotNull AnnotatedElement element) {
        return "uuid";
    }
}
