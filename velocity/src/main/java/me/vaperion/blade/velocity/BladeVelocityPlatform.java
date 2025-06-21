package me.vaperion.blade.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.BladePlatform;
import me.vaperion.blade.velocity.argument.PlayerArgument;
import me.vaperion.blade.velocity.container.VelocityContainer;
import me.vaperion.blade.velocity.platform.VelocityHelpGenerator;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public final class BladeVelocityPlatform implements BladePlatform {

    private final ProxyServer proxyServer;

    @Override
    public @NotNull Object getPluginInstance() {
        return proxyServer;
    }

    @Override
    public @NotNull ContainerCreator<?> getContainerCreator() {
        return VelocityContainer.CREATOR;
    }

    @Override
    public void configureBlade(Blade.@NotNull Builder builder, @NotNull BladeConfiguration configuration) {
        configuration.setPluginInstance(proxyServer);
        configuration.setFallbackPrefix("velocity");
        configuration.setHelpGenerator(new VelocityHelpGenerator());

        Binder binder = new Binder(builder, true);
        binder.bind(Player.class, new PlayerArgument());
    }
}
