package me.vaperion.blade.paper;

import me.vaperion.blade.Blade;
import me.vaperion.blade.bukkit.BladeBukkitPlatform;
import me.vaperion.blade.context.Sender;
import me.vaperion.blade.paper.brigadier.LegacyBladePaperBrigadier;
import me.vaperion.blade.paper.context.PaperSender;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class BladePaperPlatform extends BladeBukkitPlatform {

    private LegacyBladePaperBrigadier legacyBrigadier;

    public BladePaperPlatform(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    public @NotNull Sender<CommandSender> wrapSender(@NotNull CommandSender sender) {
        return new PaperSender(sender);
    }

    @Override
    public boolean brigadierSuggestsSubcommands(@NotNull String rootLabel) {
        return legacyBrigadier != null && legacyBrigadier.handlesRoot(rootLabel);
    }

    @Override
    public void ingestBlade(@NotNull Blade blade) {
        super.ingestBlade(blade);

        boolean success = initLegacy(blade);

        if (success) {
            blade.logger().info("Successfully hooked into Brigadier.");
        } else {
            blade.logger().info("Brigadier support not available, falling back to standard suggestions.");
        }
    }

    private boolean initLegacy(@NotNull Blade blade) {
        try {
            legacyBrigadier = new LegacyBladePaperBrigadier(blade);

            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            return false;
        } catch (Throwable t) {
            blade.logger().error(t, "Failed to initialize Legacy Brigadier support!");
            return false;
        }
    }
}
