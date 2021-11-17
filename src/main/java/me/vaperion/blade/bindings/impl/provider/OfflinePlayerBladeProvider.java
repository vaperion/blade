package me.vaperion.blade.bindings.impl.provider;

import me.vaperion.blade.argument.BladeArgument;
import me.vaperion.blade.argument.BladeProvider;
import me.vaperion.blade.bindings.impl.BukkitBindings;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.exception.BladeUsageMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OfflinePlayerBladeProvider implements BladeProvider<OfflinePlayer> {
    @Nullable
    @Override
    public OfflinePlayer provide(@NotNull BladeContext ctx, @NotNull BladeArgument arg) throws BladeExitMessage {
        Player player = ctx.sender().parseAs(Player.class);

        if (arg.getType() == BladeArgument.Type.OPTIONAL && "self".equals(arg.getString())) {
            if (player != null) return player;
            else
                throw new BladeUsageMessage(); // show usage to console if we have 'self' as a default value (only works on players)
        }

        return getOfflinePlayer(arg.getString());
    }

    @NotNull
    @Override
    public List<String> suggest(@NotNull BladeContext context, @NotNull BladeArgument arg) throws BladeExitMessage {
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
        return BukkitBindings.UUID_PATTERN.matcher(input).matches();
    }

    @NotNull
    private OfflinePlayer getOfflinePlayer(@NotNull String input) {
        if (isUUID(input)) return Bukkit.getOfflinePlayer(UUID.fromString(input));
        return Bukkit.getOfflinePlayer(input);
    }
}
