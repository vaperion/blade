package me.vaperion.blade.bukkit.argument;

import me.vaperion.blade.argument.Argument;
import me.vaperion.blade.argument.Argument.Type;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.exception.BladeUsageMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class PlayerArgument implements ArgumentProvider<Player> {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    @Nullable
    @Override
    public Player provide(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        Player player = ctx.sender().parseAs(Player.class);

        if (arg.getType() == Type.OPTIONAL && "self".equals(arg.getString())) {
            if (player != null) return player;
            else
                throw new BladeUsageMessage(); // show usage to console if we have 'self' as a default value (only works on players)
        }

        Player onlinePlayer = getPlayer(arg.getString());
        if (onlinePlayer == null && !arg.getParameter().ignoreFailedArgumentParse())
            throw new BladeExitMessage("Error: No online player with name or UUID '" + arg.getString() + "' found.");

        return onlinePlayer;
    }

    @NotNull
    @Override
    public List<String> suggest(@NotNull Context context, @NotNull Argument arg) throws BladeExitMessage {
        Player sender = context.sender().parseAs(Player.class);
        List<String> completions = new ArrayList<>();

        String input = arg.getString();

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(input.toLowerCase()) && (sender == null || sender.canSee(player)))
                completions.add(player.getName());
        }

        return completions;
    }

    private boolean isUUID(@NotNull String input) {
        return UUID_PATTERN.matcher(input).matches();
    }

    @Nullable
    private Player getPlayer(@NotNull String input) {
        if (isUUID(input)) return Bukkit.getPlayer(UUID.fromString(input));
        return Bukkit.getPlayer(input);
    }
}
