package me.vaperion.blade.bukkit.argument;

import me.vaperion.blade.annotation.parameter.Opt;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.exception.BladeUsageMessage;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.UUID;

import static me.vaperion.blade.util.BladeHelper.isUUID;

public class OfflinePlayerArgument implements ArgumentProvider<OfflinePlayer> {

    @Override
    public boolean handlesNullInputArguments() {
        return true;
    }

    @Nullable
    @Override
    public OfflinePlayer provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        Player player = ctx.sender().parseAs(Player.class);

        if (arg.isOptionalWithType(Opt.Type.SENDER) && !arg.status().isPresent()) {
            if (player != null)
                return player;

            throw new BladeUsageMessage();
        }

        if (arg.value() == null && arg.isOptionalAcceptingNull()) {
            // Since #handlesNullInputArguments returns true we need to handle this case
            return null;
        }

        return getOfflinePlayer(arg.requireValue());
    }

    @Override
    public void suggest(@NotNull Context ctx,
                        @NotNull InputArgument arg,
                        @NotNull SuggestionsBuilder suggestions) throws BladeParseError {
        Player sender = ctx.sender().parseAs(Player.class);

        String input = arg.requireValue();

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(input.toLowerCase()) &&
                (sender == null || sender.canSee(player)))
                suggestions.suggest(player.getName());
        }
    }

    @Override
    public @Nullable String defaultArgName(@NotNull AnnotatedElement element) {
        return "offline player";
    }

    @SuppressWarnings("deprecation")
    @NotNull
    private OfflinePlayer getOfflinePlayer(@NotNull String input) {
        if (isUUID(input))
            return Bukkit.getOfflinePlayer(UUID.fromString(input));

        return Bukkit.getOfflinePlayer(input);
    }

}
