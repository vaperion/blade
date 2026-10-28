package me.vaperion.blade.paper.brigadier;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import me.vaperion.blade.Blade;
import me.vaperion.blade.brigadier.BladeBrigadierBuilder;
import me.vaperion.blade.bukkit.BladeBukkitPlatform;
import me.vaperion.blade.bukkit.context.BukkitSender;
import me.vaperion.blade.tree.CommandTreeNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings({ "UnstableApiUsage" })
public final class LegacyBladePaperBrigadier implements Listener {

    private final Blade blade;
    private final BladeBrigadierBuilder<BukkitBrigadierCommandSource, CommandSender> builder;
    private final Set<String> registeredRoots = new HashSet<>();

    public LegacyBladePaperBrigadier(@NotNull Blade blade) throws ClassNotFoundException {
        Class.forName("com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent");

        this.blade = blade;

        this.builder = new BladeBrigadierBuilder<>(blade,
            BukkitBrigadierCommandSource::getBukkitSender,
            BukkitSender::new);

        Bukkit.getPluginManager().registerEvents(this,
            blade.platformAs(BladeBukkitPlatform.class).plugin());
    }

    @EventHandler
    public void onCommandRegistered(CommandRegisteredEvent<BukkitBrigadierCommandSource> event) {
        CommandTreeNode node = blade.commandTree().root(event.getCommandLabel());
        if (node == null) return;

        registeredRoots.add(event.getCommandLabel().toLowerCase(Locale.ROOT));

        event.setLiteral(builder.buildLiteral(
            node,
            event.getCommandLabel(),
            event.getBrigadierCommand(),
            event.getBrigadierCommand()
        ));
    }

    /**
     * Whether Brigadier handles subcommand suggestion instead of Blade.
     *
     * @param rootLabel the lowercase root command label
     * @return true if Brigadier suggests the root's subcommands
     */
    public boolean handlesRoot(@NotNull String rootLabel) {
        return registeredRoots.contains(rootLabel.toLowerCase(Locale.ROOT));
    }
}
