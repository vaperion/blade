package me.vaperion.blade.bukkit;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.bukkit.argument.OfflinePlayerArgument;
import me.vaperion.blade.bukkit.argument.PlayerArgument;
import me.vaperion.blade.bukkit.brigadier.BladeBrigadierSupport;
import me.vaperion.blade.bukkit.container.BukkitContainer;
import me.vaperion.blade.bukkit.context.BukkitSender;
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

import java.lang.reflect.Method;
import java.util.Locale;

@RequiredArgsConstructor
public final class BladeBukkitPlatform implements BladePlatform {

    private static final Method SYNC_COMMANDS;

    static {
        Method syncCommands = null;

        try {
            Class<?> craftServerClass = Bukkit.getServer().getClass();
            syncCommands = craftServerClass.getDeclaredMethod("syncCommands");
            syncCommands.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
            // Doesn't exist in 1.8
        } catch (Exception ex) {
            System.err.println("Failed to grab CraftServer#syncCommands method.");
            ex.printStackTrace();
        }

        SYNC_COMMANDS = syncCommands;
    }

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

    @Override
    public void ingestBlade(@NotNull Blade blade) {
        try {
            new BladeBrigadierSupport(blade, BukkitSender::new);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // No paper / brigadier not supported
        } catch (Throwable t) {
            System.err.println("Blade failed to initialize Brigadier support.");
            t.printStackTrace();
        }
    }

    @Override
    public void postCommandMapUpdate() {
        if (SYNC_COMMANDS != null) {
            try {
                SYNC_COMMANDS.invoke(Bukkit.getServer());
            } catch (Throwable t) {
                System.err.println("Blade failed to invoke CraftServer#syncCommands method, Brigadier may not recognize new commands.");
                t.printStackTrace();
            }
        }
    }
}
