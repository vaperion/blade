package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.Argument;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BooleanArgument implements ArgumentProvider<Boolean> {
    private static final Map<String, Boolean> BOOLEAN_MAP = new HashMap<>();

    static {
        BOOLEAN_MAP.put("true", true);
        BOOLEAN_MAP.put("false", false);
        BOOLEAN_MAP.put("yes", true);
        BOOLEAN_MAP.put("no", false);
        BOOLEAN_MAP.put("1", true);
        BOOLEAN_MAP.put("0", false);
    }

    @Override
    public @Nullable Boolean provide(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        Boolean bool = BOOLEAN_MAP.get(arg.getString().toLowerCase(Locale.ROOT));

        if (bool == null) {
            if (arg.getParameter().ignoreFailedArgumentParse()) return null;
            throw new BladeExitMessage("Error: '" + arg.getString() + "' is not a valid logical value.");
        }

        return bool;
    }
}
