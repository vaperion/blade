package me.vaperion.blade.bindings.impl.provider;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.vaperion.blade.argument.BladeArgument;
import me.vaperion.blade.argument.BladeProvider;
import me.vaperion.blade.bindings.impl.VelocityBindings;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.exception.BladeUsageMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VelocityPlayerBladeProvider implements BladeProvider<Player> {

    @Override
    public @Nullable Player provide(@NotNull BladeContext ctx, @NotNull BladeArgument arg) throws BladeExitMessage {
        ProxyServer proxyServer = (ProxyServer) ctx.commandService().getVelocityProxyServer();

        Player player = ctx.sender().parseAs(Player.class);

        if (arg.getType() == BladeArgument.Type.OPTIONAL && "self".equals(arg.getString())) {
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
    public @NotNull List<String> suggest(@NotNull BladeContext ctx, @NotNull BladeArgument arg) throws BladeExitMessage {
        ProxyServer proxyServer = (ProxyServer) ctx.commandService().getVelocityProxyServer();

        List<String> completions = new ArrayList<>();
        String input = arg.getString();

        for (Player player : proxyServer.getAllPlayers()) {
            if (player.getUsername().toLowerCase().startsWith(input.toLowerCase()))
                completions.add(player.getUsername());
        }

        return completions;
    }

    private boolean isUUID(@NotNull String input) {
        return VelocityBindings.UUID_PATTERN.matcher(input).matches();
    }

    @Nullable
    private Player getPlayer(@NotNull ProxyServer proxyServer, @NotNull String input) {
        if (isUUID(input)) return proxyServer.getPlayer(UUID.fromString(input)).orElse(null);
        return proxyServer.getPlayer(input).orElse(null);
    }
}
