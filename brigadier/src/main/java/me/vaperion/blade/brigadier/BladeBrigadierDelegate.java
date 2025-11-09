package me.vaperion.blade.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.container.Container;
import me.vaperion.blade.tree.CommandTreeNode;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public final class BladeBrigadierDelegate<S, C extends Container> {

    private final Blade blade;
    private final DelegateSuggestionProvider<S, C> suggestionProvider;
    private final DelegateExecutor<S, C> executor;

    @NotNull
    public SuggestionProvider<S> suggestionProvider(@NotNull CommandTreeNode node) {
        @SuppressWarnings("unchecked")
        C container = (C) node.container();

        if (container == null) {
            return (ctx, builder) -> {
                blade.logger().warn("Cannot provide suggestions for command `%s`: no container found. This is most likely a bug in Blade, not your plugin. Please report it.",
                    ctx.getInput());

                return builder.buildFuture();
            };
        }

        return (ctx, builder) -> {
            try {
                suggestionProvider.suggest(ctx, builder, container);
            } catch (Throwable t) {
                blade.logger().error(t, "An error occurred while providing suggestions for command `%s`",
                    ctx.getInput());
            }

            return builder.buildFuture();
        };
    }

    @NotNull
    public Command<S> executor(@NotNull CommandTreeNode node) {
        @SuppressWarnings("unchecked")
        C container = (C) node.container();

        if (container == null) {
            return ctx -> {
                blade.logger().warn("Cannot execute command `%s`: no container found. This is most likely a bug in Blade, not your plugin. Please report it.",
                    ctx.getInput());

                return 0;
            };
        }

        return ctx -> {
            try {
                boolean success = executor.execute(ctx, container);
                return success ? Command.SINGLE_SUCCESS : 0;
            } catch (Throwable t) {
                blade.logger().error(t, "An error occurred while executing command `%s`",
                    ctx.getInput());

                return 0;
            }
        };
    }

    @FunctionalInterface
    public interface DelegateSuggestionProvider<S, C extends Container> {
        void suggest(@NotNull CommandContext<S> context,
                     @NotNull SuggestionsBuilder builder,
                     @NotNull C container) throws Throwable;
    }

    @FunctionalInterface
    public interface DelegateExecutor<S, C extends Container> {
        boolean execute(@NotNull CommandContext<S> context,
                        @NotNull C container) throws Throwable;
    }

}
