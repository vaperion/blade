package me.vaperion.blade.paper.brigadier;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.brigadier.BladeBrigadierBuilder;
import me.vaperion.blade.brigadier.BladeBrigadierDelegate;
import me.vaperion.blade.bukkit.container.BukkitContainer;
import me.vaperion.blade.bukkit.context.BukkitSender;
import me.vaperion.blade.impl.node.ResolvedCommandNode;
import me.vaperion.blade.paper.BladePaperPlatform;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import static me.vaperion.blade.util.BladeHelper.removeCommandQualifier;

@SuppressWarnings("UnstableApiUsage")
@Getter
public final class BladePaperBrigadier implements Listener {

    private final Blade blade;
    private final BladeBrigadierBuilder<CommandSourceStack, CommandSender> builder;
    private final BladeBrigadierDelegate<CommandSourceStack, BukkitContainer> delegate;

    public BladePaperBrigadier(@NotNull Blade blade) throws ClassNotFoundException,
                                                            NoSuchFieldException,
                                                            NoSuchMethodException {
        String ignored = LifecycleEvents.COMMANDS.name();

        this.blade = blade;

        this.builder = new BladeBrigadierBuilder<>(blade,
            CommandSourceStack::getSender,
            BukkitSender::new);

        this.delegate = new BladeBrigadierDelegate<>(blade,
            (ctx, builder, container) -> {
                for (String suggestion : container.tabComplete(sender(ctx), input(ctx))) {
                    builder.suggest(suggestion);
                }
            },
            (ctx, container) ->
                container.execute(sender(ctx), input(ctx))
        );

        blade.platformAs(BladePaperPlatform.class).plugin()
            .getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                event -> {
                    registerCommands(event.registrar());
                }
            );
    }

    @NotNull
    private CommandSender sender(@NotNull CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }

    @NotNull
    private String input(@NotNull CommandContext<CommandSourceStack> ctx) {
        String input = ctx.getInput();

        if (input.startsWith("/"))
            input = input.substring(1);

        return removeCommandQualifier(input);
    }

    private void registerCommands(@NotNull Commands registrar) {
        blade.labelToCommands().forEach((label, commands) -> {
            ResolvedCommandNode node = blade.nodeResolver().resolve(label);

            if (node == null)
                return;

            if (node.command() == null && node.subcommands().isEmpty())
                return;

            LiteralCommandNode<CommandSourceStack> literal = builder.buildLiteral(
                node,
                label,
                delegate.suggestionProvider(node),
                delegate.executor(node)
            );

            registrar.register(literal);
        });
    }
}
