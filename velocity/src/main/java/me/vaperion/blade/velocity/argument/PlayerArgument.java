package me.vaperion.blade.velocity.argument;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.vaperion.blade.argument.Argument;
import me.vaperion.blade.argument.Argument.Type;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.exception.BladeUsageMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class PlayerArgument implements ArgumentProvider<Player> {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    @Override
    public @Nullable Player provide(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        ProxyServer proxyServer = (ProxyServer) ctx.blade().getPlatform().getPluginInstance();

        Player player = ctx.sender().parseAs(Player.class);

        if (arg.getType() == Type.OPTIONAL && "self".equals(arg.getString())) {
            if (player != null) return player;
            else
                throw new BladeUsageMessage(); // show usage to console if we have 'self' as a default value (only works on players)
        }

        Player onlinePlayer = getPlayer(proxyServer, arg.getString());
        if (onlinePlayer == null && !arg.getParameter().ignoreFailedArgumentParse())
            throw new BladeExitMessage("Error: No online player with name or UUID '" + arg.getString() + "' found.");

        return onlinePlayer;
    }

    @Override
    public @NotNull
    List<String> suggest(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        ProxyServer proxyServer = (ProxyServer) ctx.blade().getPlatform().getPluginInstance();

        List<String> completions = new ArrayList<>();
        String input = arg.getString();

        for (Player player : proxyServer.getAllPlayers()) {
            if (player.getUsername().toLowerCase().startsWith(input.toLowerCase()))
                completions.add(player.getUsername());
        }

        return completions;
    }

    private boolean isUUID(@NotNull String input) {
        return UUID_PATTERN.matcher(input).matches();
    }

    @Nullable
    private Player getPlayer(@NotNull ProxyServer proxyServer, @NotNull String input) {
        if (isUUID(input)) return proxyServer.getPlayer(UUID.fromString(input)).orElse(null);
        return proxyServer.getPlayer(input).orElse(null);
    }

}
