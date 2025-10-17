package me.vaperion.blade.velocity.argument;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.vaperion.blade.annotation.parameter.Opt;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.exception.BladeUsageMessage;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import me.vaperion.blade.velocity.BladeVelocityPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.regex.Pattern;

public class PlayerArgument implements ArgumentProvider<Player> {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    @Override
    public boolean handlesNullInputArguments() {
        return true;
    }

    @Override
    public @Nullable Player provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        ProxyServer proxyServer = ctx.blade().platformAs(BladeVelocityPlatform.class).server();

        Player player = ctx.sender().parseAs(Player.class);

        if (arg.isOptionalWithType(Opt.Type.SENDER) && !arg.status().isPresent()) {
            if (player != null)
                return player;

            throw new BladeUsageMessage();
        }

        Player onlinePlayer = getPlayer(proxyServer, arg.requireValue());

        if (onlinePlayer == null) {
            throw BladeParseError.recoverable(String.format(
                "No online player found with name or UUID '%s'",
                arg.value()
            ));
        }

        return onlinePlayer;
    }

    @Override
    public void suggest(@NotNull Context ctx,
                        @NotNull InputArgument arg,
                        @NotNull SuggestionsBuilder suggestions) throws BladeParseError {
        ProxyServer proxyServer = ctx.blade().platformAs(BladeVelocityPlatform.class).server();

        String input = arg.requireValue();

        for (Player player : proxyServer.getAllPlayers()) {
            if (player.getUsername().toLowerCase().startsWith(input.toLowerCase()))
                suggestions.suggest(player.getUsername());
        }
    }

    private boolean isUUID(@NotNull String input) {
        return UUID_PATTERN.matcher(input).matches();
    }

    @Nullable
    private Player getPlayer(@NotNull ProxyServer proxyServer, @NotNull String input) {
        if (isUUID(input))
            return proxyServer.getPlayer(UUID.fromString(input)).orElse(null);

        return proxyServer.getPlayer(input).orElse(null);
    }

}
