package me.vaperion.blade.paper;

import me.vaperion.blade.Blade;
import me.vaperion.blade.bukkit.BladeBukkitPlatform;
import me.vaperion.blade.impl.suggestions.SuggestionType;
import me.vaperion.blade.paper.brigadier.BladePaperBrigadier;
import me.vaperion.blade.paper.brigadier.LegacyBladePaperBrigadier;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class BladePaperPlatform extends BladeBukkitPlatform {

    public BladePaperPlatform(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    public void ingestBlade(@NotNull Blade blade) {
        super.ingestBlade(blade);

        boolean success = init(blade);

        if (!success)
            success = initLegacy(blade);

        if (success) {
            // brigadier handles this
            this.suggestionTypes.remove(SuggestionType.SUBCOMMANDS);

            blade.logger().info("Successfully hooked into Brigadier.");
        } else {
            blade.logger().info("Brigadier support not available, falling back to standard suggestions.");
        }
    }

    private boolean init(@NotNull Blade blade) {
        try {
            new BladePaperBrigadier(blade);

            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError
                 | NoSuchFieldException | NoSuchFieldError
                 | NoSuchMethodException | NoSuchMethodError ignored) {
            return false;
        } catch (Throwable t) {
            blade.logger().error(t, "Failed to initialize Brigadier support!");
            return false;
        }
    }

    private boolean initLegacy(@NotNull Blade blade) {
        try {
            new LegacyBladePaperBrigadier(blade);

            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            return false;
        } catch (Throwable t) {
            blade.logger().error(t, "Failed to initialize Legacy Brigadier support!");
            return false;
        }
    }
}
