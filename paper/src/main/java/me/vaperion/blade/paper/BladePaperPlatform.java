package me.vaperion.blade.paper;

import me.vaperion.blade.Blade;
import me.vaperion.blade.bukkit.BladeBukkitPlatform;
import me.vaperion.blade.impl.suggestions.SuggestionType;
import me.vaperion.blade.paper.brigadier.BladeBrigadierSupport;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class BladePaperPlatform extends BladeBukkitPlatform {

    public BladePaperPlatform(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void ingestBlade(@NotNull Blade blade) {
        try {
            new BladeBrigadierSupport(blade);

            this.suggestionTypes.remove(SuggestionType.SUBCOMMANDS); // brigadier handles this
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // No paper / brigadier not supported
        } catch (Throwable t) {
            blade.logger().error(t, "Failed to initialize Brigadier support!");
        }
    }
}
