package me.vaperion.blade.bukkit;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.bukkit.argument.OfflinePlayerArgument;
import me.vaperion.blade.bukkit.argument.PlayerArgument;
import me.vaperion.blade.bukkit.container.BukkitContainer;
import me.vaperion.blade.bukkit.platform.BukkitHelpGenerator;
import me.vaperion.blade.bukkit.platform.ProtocolLibTabCompleter;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.BladePlatform;
import me.vaperion.blade.platform.TabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@RequiredArgsConstructor
public final class BladeBukkitPlatform implements BladePlatform {

    private final JavaPlugin plugin;

    @Override
    public @NotNull Object getPluginInstance() {
        return plugin;
    }

    @Override
    public @NotNull ContainerCreator<?> getContainerCreator() {
        return BukkitContainer.CREATOR;
    }

    @Override
    public void configureBlade(Blade.@NotNull Builder builder, @NotNull BladeConfiguration configuration) {
        configuration.setPluginInstance(plugin);
        configuration.setFallbackPrefix(plugin.getName().toLowerCase(Locale.ROOT));
        configuration.setHelpGenerator(new BukkitHelpGenerator());
        configuration.setTabCompleter(Bukkit.getPluginManager().isPluginEnabled("ProtocolLib") ? new ProtocolLibTabCompleter(plugin) : new TabCompleter.Default());

        Binder binder = new Binder(builder, true);
        binder.bind(Player.class, new PlayerArgument());
        binder.bind(OfflinePlayer.class, new OfflinePlayerArgument());
    }
}
