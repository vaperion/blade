package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.Argument;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UUIDArgument implements ArgumentProvider<UUID> {
    @Override
    public @Nullable UUID provide(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        try {
            return UUID.fromString(arg.getString());
        } catch (Exception ex) {
            if (arg.getParameter().ignoreFailedArgumentParse()) return null;
            throw new BladeExitMessage("Error: '" + arg.getString() + "' is not a valid UUID.");
        }
    }
}
