package me.vaperion.blade.minestom;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.minestom.api.PermissionChecker;
import me.vaperion.blade.minestom.argument.PlayerArgument;
import me.vaperion.blade.minestom.command.MinestomCommandFeedback;
import me.vaperion.blade.minestom.container.MinestomContainer;
import me.vaperion.blade.minestom.platform.MinestomHelpGenerator;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.BladePlatform;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@RequiredArgsConstructor
public class BladeMinestomPlatform implements BladePlatform<Component, MinecraftServer, MinecraftServer> {

    protected final MinecraftServer server;
    protected final PermissionChecker permissionChecker;

    protected Blade blade;

    @Override
    public void ingestBlade(@NotNull Blade blade) {
        this.blade = blade;

        BladeMinestomGlobal.setServer(server);
        BladeMinestomGlobal.setPermissionChecker(permissionChecker);
    }

    @Override
    public @NotNull MinecraftServer server() {
        return server;
    }

    @Override
    public @NotNull MinecraftServer plugin() {
        return server;
    }

    @Override
    public @NotNull ContainerCreator<?> containerCreator(@NotNull BladeCommand command) {
        return MinestomContainer.CREATOR;
    }

    @Override
    public void configure(Blade.@NotNull Builder<Component, MinecraftServer, MinecraftServer> builder,
                          @NotNull BladeConfiguration<Component> configuration) {
        configuration.helpGenerator(new MinestomHelpGenerator());
        configuration.feedbackCreator(MinestomCommandFeedback::new);

        Binder<Component, MinecraftServer, MinecraftServer> binder = new Binder<>(builder, true);
        binder.bind(Player.class, new PlayerArgument());
    }

    @Override
    public @NotNull String convertSenderTypeToName(@NotNull Class<?> type, boolean plural) {
        if (Player.class.isAssignableFrom(type)) {
            return plural ? "players" : "player";
        } else if (ConsoleSender.class.isAssignableFrom(type)) {
            return "console";
        } else {
            // Fallback
            String name = type.getSimpleName().toLowerCase(Locale.ROOT);
            return plural ? name + "s" : name;
        }
    }
}
