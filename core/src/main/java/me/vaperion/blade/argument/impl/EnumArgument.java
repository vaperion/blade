package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.Argument;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumArgument implements ArgumentProvider<Enum> {

    private static Class<? extends Enum> enumClass;

    @Override
    public @Nullable Enum provide(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        enumClass = (Class<? extends Enum>) arg.getParameter().getType();

        Enum value = null;
        try {
            value = Enum.valueOf(enumClass, arg.getString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        if (value == null && !arg.getParameter().ignoreFailedArgumentParse())
            throw new BladeExitMessage("Error: '" + arg.getString() + "' is not a valid enum value.");

        return value;
    }

    @Override
    public @NotNull List<String> suggest(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        enumClass = (Class<? extends Enum>) arg.getParameter().getType();

        String input = arg.getString().toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        for (Enum value : enumClass.getEnumConstants()) {
            String name = value.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(input)) completions.add(name);
        }

        return completions;
    }
}
