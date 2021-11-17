package me.vaperion.blade.bindings.impl.provider;

import me.vaperion.blade.argument.BladeArgument;
import me.vaperion.blade.argument.BladeProvider;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.exception.BladeExitMessage;
import org.bukkit.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GameModeBladeProvider implements BladeProvider<GameMode> {
    @Nullable
    @Override
    public GameMode provide(@NotNull BladeContext ctx, @NotNull BladeArgument arg) throws BladeExitMessage {
        GameMode mode = getGameMode(arg.getString());

        if (mode == null && !arg.getParameter().ignoreFailedArgumentParse())
            throw new BladeExitMessage("Error: '" + arg.getString() + "' is not a valid gamemode.");

        return mode;
    }

    @NotNull
    @Override
    public List<String> suggest(@NotNull BladeContext context, @NotNull BladeArgument arg) throws BladeExitMessage {
        String input = arg.getString().toUpperCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        for (GameMode mode : GameMode.values()) {
            if (mode.name().startsWith(input)) {
                completions.add(mode.name().toLowerCase(Locale.ROOT));
            }
        }

        return completions;
    }

    @Nullable
    private GameMode getGameMode(String input) {
        input = input.toUpperCase(Locale.ROOT);

        for (GameMode mode : GameMode.values()) {
            if (mode.name().startsWith(input) || input.equals(String.valueOf(mode.getValue()))) {
                return mode;
            }
        }

        return null;
    }
}
