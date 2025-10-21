package me.vaperion.blade.fabric.container;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.vaperion.blade.Blade;
import me.vaperion.blade.brigadier.BladeBrigadierBuilder;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.Container;
import me.vaperion.blade.fabric.context.FabricSender;
import me.vaperion.blade.impl.node.ResolvedCommandNode;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class BladeFabricBrigadier {

    private final Blade blade;
    private final BladeBrigadierBuilder<ServerCommandSource, ServerCommandSource> builder;

    public BladeFabricBrigadier(@NotNull Blade blade) {
        this.blade = blade;
        this.builder = new BladeBrigadierBuilder<>(blade,
            Function.identity(),
            s -> new FabricSender(blade, s));
    }

    @NotNull
    public BladeBrigadierBuilder<ServerCommandSource, ServerCommandSource> getBuilder() {
        return builder;
    }


    @NotNull
    public SuggestionProvider<ServerCommandSource> delegatingSuggestionProvider(
        @NotNull ResolvedCommandNode node) {
        FabricContainer container = findContainer(node);

        if (container == null) {
            return (ctx, builder) ->
                builder.buildFuture();
        }

        return (ctx, builder) -> {
            try {
                container.suggest(ctx, builder);
            } catch (Throwable t) {
                blade.logger().error(t, "An error occurred while providing suggestions for command `%s`", container.label());
            }

            return builder.buildFuture();
        };
    }

    @NotNull
    public Command<ServerCommandSource> delegatingExecutor(@NotNull ResolvedCommandNode node) {
        FabricContainer container = findContainer(node);

        if (container == null) {
            return ctx -> 0;
        }

        return ctx -> {
            try {
                boolean success = container.execute(ctx);
                return success ? Command.SINGLE_SUCCESS : 0;
            } catch (Throwable t) {
                blade.logger().error(t, "An error occurred while executing command `%s`", container.label());
                return 0;
            }
        };
    }

    @Nullable
    private FabricContainer findContainer(@NotNull ResolvedCommandNode node) {
        if (node.command() != null) {
            FabricContainer container = findContainerInner(node.command());
            if (container != null)
                return container;
        }

        for (ResolvedCommandNode subcommand : node.subcommands()) {
            FabricContainer container = findContainer(subcommand);

            if (container != null)
                return container;
        }

        return null;
    }

    @Nullable
    private FabricContainer findContainerInner(@NotNull BladeCommand command) {
        for (String label : command.labels()) {
            String realLabel = label.split(" ")[0];

            Container container = blade.labelToContainer().get(realLabel);

            if (container != null) {
                return (FabricContainer) container;
            }
        }

        return null;
    }
}
