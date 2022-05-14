package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.Argument;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringArgument implements ArgumentProvider<String> {
    @Override
    public @Nullable String provide(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        if (arg.getString() == null) {
            if (arg.getParameter().ignoreFailedArgumentParse()) return null;
            throw new BladeExitMessage("Error: '" + arg.getString() + "' is not a valid string.");
        }

        return arg.getString();
    }
}
