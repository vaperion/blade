package me.vaperion.blade.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.parameter.Range;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.BladeParameter;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.context.Sender;
import me.vaperion.blade.tree.CommandTreeNode;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("ClassCanBeRecord")
@RequiredArgsConstructor
public final class BladeBrigadierBuilder<T, S> {

    private final Blade blade;
    private final Function<T, S> converter;
    private final Function<S, Sender<S>> wrapper;

    @NotNull
    public LiteralCommandNode<T> buildLiteral(
        @NotNull CommandTreeNode node,
        @NotNull String label,
        @NotNull SuggestionProvider<T> suggestionProvider,
        @NotNull Command<T> executor) {
        LiteralArgumentBuilder<T> builder = LiteralArgumentBuilder.<T>literal(label)
            .requires(createPermissionPredicate(node))
            .executes(executor);

        LiteralCommandNode<T> root = builder.build();

        if (node.isLeaf()) {
            registerParams(node, root, suggestionProvider, executor);
        }

        for (CommandTreeNode subcommand : node.children().values()) {
            registerSubCommand(root,
                subcommand,
                suggestionProvider,
                executor);
        }

        return root;
    }

    private void registerSubCommand(
        @NotNull LiteralCommandNode<T> root,
        @NotNull CommandTreeNode node,
        @NotNull SuggestionProvider<T> suggestionProvider,
        @NotNull Command<T> executor) {
        String label = node.label();

        LiteralArgumentBuilder<T> builder = LiteralArgumentBuilder.<T>literal(label)
            .requires(createPermissionPredicate(node))
            .executes(executor);

        LiteralCommandNode<T> subcommandNode = builder.build();
        root.addChild(subcommandNode);

        if (node.isLeaf()) {
            registerParams(node, subcommandNode, suggestionProvider, executor);
        }

        for (CommandTreeNode child : node.children().values()) {
            registerSubCommand(subcommandNode, child, suggestionProvider, executor);
        }
    }

    private void registerParams(@NotNull CommandTreeNode node,
                                @NotNull CommandNode<T> commandNode,
                                @NotNull SuggestionProvider<T> suggestionProvider,
                                @NotNull Command<T> brigadierCommand) {
        boolean hasGreedy = false;

        for (DefinedArgument arg : node.command().arguments()) {
            RequiredArgumentBuilder<T, ?> builder = RequiredArgumentBuilder
                .<T, Object>argument(arg.name(), mapBrigadierArgument(node.command(), arg))
                .suggests(suggestionProvider)
                .requires(createPermissionPredicate(node))
                .executes(brigadierCommand);

            if (builder.getType() instanceof StringArgumentType stringType) {
                if (stringType.getType() == StringArgumentType.StringType.GREEDY_PHRASE) {
                    hasGreedy = true;
                }
            }

            CommandNode<T> argument = builder.build();
            commandNode.addChild(argument);
            commandNode = argument;
        }

        if (!node.command().flags().isEmpty()) {
            // We add an argument to the end so the user can pass flags.
            // This is not a great solution, and we can't do it if there's already a greedy argument.
            // Not sure how to improve this right now.

            if (hasGreedy)
                return;

            RequiredArgumentBuilder<T, String> builder = RequiredArgumentBuilder
                .<T, String>argument("flags", StringArgumentType.greedyString())
                .suggests(suggestionProvider)
                .requires(createPermissionPredicate(node))
                .executes(brigadierCommand);

            CommandNode<T> argument = builder.build();
            commandNode.addChild(argument);
        }
    }

    @NotNull
    private Predicate<T> createPermissionPredicate(@NotNull CommandTreeNode node) {
        return sender -> {
            Sender<?> wrappedSender = wrapper.apply(converter.apply(sender));
            Context context = new Context(blade, wrappedSender, "", new String[0]);

            if (node.command() != null) {
                return node.command().hasPermission(context);
            }

            if (node.isStub()) {
                return hasAccessibleCommand(node, context);
            }

            return true;
        };
    }

    private boolean hasAccessibleCommand(@NotNull CommandTreeNode node,
                                         @NotNull Context context) {
        if (node.isLeaf()) {
            BladeCommand cmd = node.command();
            if (cmd.hasPermission(context)) {
                return true;
            }
        }

        for (CommandTreeNode child : node.children().values()) {
            if (hasAccessibleCommand(child, context)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    private ArgumentType<Object> mapBrigadierArgument(@NotNull BladeCommand command,
                                                      @NotNull BladeParameter parameter) {
        Class<?> clazz = parameter.type();
        ArgumentType<?> type = StringArgumentType.string();

        if (clazz == String.class) {
            if (parameter.isGreedy()) type = StringArgumentType.greedyString();
            else if (command.parseQuotes()) type = StringArgumentType.string();
            else type = StringArgumentType.word();
        }

        if (clazz == int.class || clazz == Integer.class) {
            if (parameter.hasRange()) {
                Range range = parameter.range();
                assert range != null;

                int min = Double.isNaN(range.min())
                    ? Integer.MIN_VALUE
                    : (int) Math.floor(range.min());
                int max = Double.isNaN(range.max())
                    ? Integer.MAX_VALUE
                    : (int) Math.ceil(range.max());

                type = IntegerArgumentType.integer(min, max);
            } else type = IntegerArgumentType.integer();
        }

        if (clazz == float.class || clazz == Float.class) {
            if (parameter.hasRange()) {
                Range range = parameter.range();
                assert range != null;

                float min = Double.isNaN(range.min())
                    ? Float.MIN_VALUE
                    : (float) range.min();
                float max = Double.isNaN(range.max())
                    ? Float.MAX_VALUE
                    : (float) range.max();

                type = FloatArgumentType.floatArg(min, max);
            } else type = FloatArgumentType.floatArg();
        }

        if (clazz == double.class || clazz == Double.class) {
            if (parameter.hasRange()) {
                Range range = parameter.range();
                assert range != null;

                double min = Double.isNaN(range.min())
                    ? Double.MIN_VALUE
                    : range.min();
                double max = Double.isNaN(range.max())
                    ? Double.MAX_VALUE
                    : range.max();

                type = DoubleArgumentType.doubleArg(min, max);
            } else type = DoubleArgumentType.doubleArg();
        }

        if (clazz == long.class || clazz == Long.class) {
            if (parameter.hasRange()) {
                Range range = parameter.range();
                assert range != null;

                long min = Double.isNaN(range.min())
                    ? Long.MIN_VALUE
                    : (long) Math.floor(range.min());
                long max = Double.isNaN(range.max())
                    ? Long.MAX_VALUE
                    : (long) Math.ceil(range.max());

                type = LongArgumentType.longArg(min, max);
            } else type = LongArgumentType.longArg();
        }

        if (clazz == boolean.class || clazz == Boolean.class) {
            // we use word because we support custom true/false values like `on`/`off` and `yes`/`no`,
            // and brigadier is not flexible enough to support that natively
            type = StringArgumentType.word();
        }

        //noinspection unchecked
        return (ArgumentType<Object>) type;
    }

}
