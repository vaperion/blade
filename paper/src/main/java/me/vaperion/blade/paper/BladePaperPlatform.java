package me.vaperion.blade.paper;

import me.vaperion.blade.Blade;
import me.vaperion.blade.bukkit.BladeBukkitPlatform;
import me.vaperion.blade.bukkit.context.BukkitSender;
import me.vaperion.blade.paper.brigadier.BladeBrigadierSupport;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
            System.err.println("Blade failed to initialize Brigadier support.");
            t.printStackTrace();
        }
    }
}
