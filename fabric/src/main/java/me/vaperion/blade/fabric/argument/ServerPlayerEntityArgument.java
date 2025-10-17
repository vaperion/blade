package me.vaperion.blade.fabric.argument;

import me.vaperion.blade.annotation.parameter.Opt;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.exception.BladeUsageMessage;
import me.vaperion.blade.fabric.BladeFabricPlatform;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.regex.Pattern;

public class ServerPlayerEntityArgument implements ArgumentProvider<ServerPlayerEntity> {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    @Override
    public boolean handlesNullInputArguments() {
        return true;
    }

    @Nullable
    @Override
    public ServerPlayerEntity provide(@NotNull Context ctx,
                                      @NotNull InputArgument arg) throws BladeParseError {
        MinecraftServer server = ctx.blade().platformAs(BladeFabricPlatform.class).server();

        ServerPlayerEntity player = ctx.sender().parseAs(ServerPlayerEntity.class);

        if (arg.isOptionalWithType(Opt.Type.SENDER) && !arg.status().isPresent()) {
            if (player != null)
                return player;

            throw new BladeUsageMessage();
        }

        ServerPlayerEntity onlinePlayer = getPlayer(server, arg.requireValue());

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
        MinecraftServer server = ctx.blade().platformAs(BladeFabricPlatform.class).server();

        ServerPlayerEntity sender = ctx.sender().parseAs(ServerPlayerEntity.class);

        String input = arg.requireValue();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getNameForScoreboard().toLowerCase().startsWith(input.toLowerCase()) &&
                (sender == null || sender.canSee(player)))
                suggestions.suggest(player.getNameForScoreboard());
        }
    }

    private boolean isUUID(@NotNull String input) {
        return UUID_PATTERN.matcher(input).matches();
    }

    @Nullable
    private ServerPlayerEntity getPlayer(@NotNull MinecraftServer server, @NotNull String input) {
        if (isUUID(input)) return server.getPlayerManager().getPlayer(UUID.fromString(input));
        return server.getPlayerManager().getPlayer(input);
    }
}
