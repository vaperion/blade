package me.vaperion.blade.paper;

import me.vaperion.blade.Blade;
import me.vaperion.blade.bukkit.BladeBukkitPlatform;
import me.vaperion.blade.bukkit.context.BukkitSender;
import me.vaperion.blade.paper.brigadier.BladeBrigadierSupport;
import me.vaperion.blade.paper.platform.NewProtocolLibTabCompleter;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.TabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class BladePaperPlatform extends BladeBukkitPlatform {

    public BladePaperPlatform(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public void ingestBlade(@NotNull Blade blade) {
        try {
            new BladeBrigadierSupport(blade, BukkitSender::new);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // No paper / brigadier not supported
        } catch (Throwable t) {
            blade.logger().error(t, "Failed to initialize Brigadier support!");
        }
    }

    @Override
    public void configureTabCompleter(@NotNull BladeConfiguration configuration) {
        configuration.setTabCompleter(Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")
            ? new NewProtocolLibTabCompleter(plugin)
            : new TabCompleter.Default());
    }
}
