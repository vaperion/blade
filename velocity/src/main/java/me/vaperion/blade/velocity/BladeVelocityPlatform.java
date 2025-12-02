package me.vaperion.blade.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.BladePlatform;
import me.vaperion.blade.velocity.argument.PlayerArgument;
import me.vaperion.blade.velocity.container.VelocityContainer;
import me.vaperion.blade.velocity.platform.VelocityHelpGenerator;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public final class BladeVelocityPlatform implements BladePlatform<Component, PluginContainer, ProxyServer> {

    private final PluginContainer pluginContainer;
    private final ProxyServer proxyServer;

    @Override
    public @NotNull ProxyServer server() {
        return proxyServer;
    }

    @Override
    public @NotNull PluginContainer plugin() {
        return pluginContainer;
    }

    @Override
    public @NotNull ContainerCreator<?> containerCreator(@NotNull BladeCommand command) {
        return VelocityContainer.CREATOR;
    }

    @Override
    public void configure(Blade.@NotNull Builder<Component, PluginContainer, ProxyServer> builder,
                          @NotNull BladeConfiguration<Component> configuration) {
        configuration.commandQualifier(pluginContainer.getDescription().getId());
        configuration.helpGenerator(new VelocityHelpGenerator());

        Binder<Component, PluginContainer, ProxyServer> binder = new Binder<>(builder, true);
        binder.bind(Player.class, new PlayerArgument());
    }

    @Override
    public @NotNull String convertSenderTypeToName(@NotNull Class<?> type, boolean plural) {
        if (ConsoleCommandSource.class.isAssignableFrom(type)) {
            return "console";
        } else if (Player.class.isAssignableFrom(type)) {
            return plural ? "players" : "player";
        } else {
            // Fallback
            String name = type.getSimpleName().toLowerCase(Locale.ROOT);
            return plural ? name + "s" : name;
        }
    }
}
