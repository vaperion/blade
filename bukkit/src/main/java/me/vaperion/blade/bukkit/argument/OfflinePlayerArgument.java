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

import java.util.UUID;
import java.util.regex.Pattern;

public class OfflinePlayerArgument implements ArgumentProvider<OfflinePlayer> {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

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

    private boolean isUUID(@NotNull String input) {
        return UUID_PATTERN.matcher(input).matches();
    }

    @SuppressWarnings("deprecation")
    @NotNull
    private OfflinePlayer getOfflinePlayer(@NotNull String input) {
        if (isUUID(input))
            return Bukkit.getOfflinePlayer(UUID.fromString(input));

        return Bukkit.getOfflinePlayer(input);
    }

}
