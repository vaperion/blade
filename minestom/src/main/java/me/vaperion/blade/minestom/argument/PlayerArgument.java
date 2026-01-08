package me.vaperion.blade.minestom.argument;

import me.vaperion.blade.annotation.parameter.Opt;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.exception.BladeUsageMessage;
import me.vaperion.blade.minestom.BladeMinestomGlobal;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.UUID;
import java.util.regex.Pattern;

public class PlayerArgument implements ArgumentProvider<Player> {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    @Override
    public boolean handlesNullInputArguments() {
        return true;
    }

    @Nullable
    @Override
    public Player provide(@NotNull Context ctx,
                          @NotNull InputArgument arg) throws BladeParseError {
        MinecraftServer server = BladeMinestomGlobal.server();

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

        Player onlinePlayer = getPlayer(server, arg.requireValue());

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
        String input = arg.requireValue();

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (player.getUsername().toLowerCase().startsWith(input.toLowerCase()))
                suggestions.suggest(player.getUsername());
        }
    }

    @Override
    public @Nullable String defaultArgName(@NotNull AnnotatedElement element) {
        return "player";
    }

    private boolean isUUID(@NotNull String input) {
        return UUID_PATTERN.matcher(input).matches();
    }

    @Nullable
    private Player getPlayer(@NotNull MinecraftServer server, @NotNull String input) {
        if (isUUID(input)) {
            return MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(
                UUID.fromString(input)
            );
        }

        return MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(input);
    }
}
