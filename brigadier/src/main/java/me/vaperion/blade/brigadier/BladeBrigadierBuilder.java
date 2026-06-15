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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        registerNodeData(node, root, suggestionProvider, executor, false);

        for (CommandTreeNode subcommand : node.children().values()) {
            registerSubCommand(root,
                subcommand,
                suggestionProvider,
                executor);
        }

        if (hasHiddenCommand(node)) {
            BrigadierCompat.setClientNode(
                root,
                buildClientLiteral(node, label, suggestionProvider, executor)
            );
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
        registerNodeData(node, subcommandNode, suggestionProvider, executor, false);

        for (CommandTreeNode child : node.children().values()) {
            registerSubCommand(subcommandNode, child, suggestionProvider, executor);
        }
    }

    @NotNull
    private LiteralCommandNode<T> buildClientLiteral(@NotNull CommandTreeNode node,
                                                     @NotNull String label,
                                                     @NotNull SuggestionProvider<T> suggestionProvider,
                                                     @NotNull Command<T> executor) {
        boolean visibleLeaf = hasVisibleLeaf(node);

        LiteralArgumentBuilder<T> builder = LiteralArgumentBuilder.<T>literal(label)
            .requires(createClientVisibilityPredicate(node));

        if (visibleLeaf) {
            builder.executes(executor);
        }

        LiteralCommandNode<T> root = builder.build();

        if (visibleLeaf) {
            registerNodeData(node, root, suggestionProvider, executor, true);
        }

        for (CommandTreeNode subcommand : node.children().values()) {
            if (!hasVisibleCommand(subcommand)) {
                continue;
            }

            root.addChild(buildClientSubCommand(subcommand, suggestionProvider, executor));
        }

        return root;
    }

    @NotNull
    private LiteralCommandNode<T> buildClientSubCommand(@NotNull CommandTreeNode node,
                                                        @NotNull SuggestionProvider<T> suggestionProvider,
                                                        @NotNull Command<T> executor) {
        String label = node.label();
        boolean visibleLeaf = hasVisibleLeaf(node);

        LiteralArgumentBuilder<T> builder = LiteralArgumentBuilder.<T>literal(label)
            .requires(createClientVisibilityPredicate(node));

        if (visibleLeaf) {
            builder.executes(executor);
        }

        LiteralCommandNode<T> subcommandNode = builder.build();

        if (visibleLeaf) {
            registerNodeData(node, subcommandNode, suggestionProvider, executor, true);
        }

        for (CommandTreeNode child : node.children().values()) {
            if (!hasVisibleCommand(child)) {
                continue;
            }

            subcommandNode.addChild(buildClientSubCommand(child, suggestionProvider, executor));
        }

        return subcommandNode;
    }

    private void registerNodeData(@NotNull CommandTreeNode node,
                                  @NotNull CommandNode<T> commandNode,
                                  @NotNull SuggestionProvider<T> suggestionProvider,
                                  @NotNull Command<T> brigadierCommand,
                                  boolean visibleOnly) {
        if (node.isLeaf()) {
            for (BladeCommand command : brigadierRegistrationOrder(node.commands())) {
                if (visibleOnly && !command.shouldSendToClient()) {
                    continue;
                }

                registerParams(command, node, commandNode, suggestionProvider, brigadierCommand, visibleOnly);
            }
        } else if (blade.configuration().registerDefaultHelpArguments() && !node.children().isEmpty()) {
            registerHelpParams(node, commandNode, suggestionProvider, brigadierCommand);
        }
    }

    private boolean hasVisibleLeaf(@NotNull CommandTreeNode node) {
        for (BladeCommand command : node.commands()) {
            if (command.shouldSendToClient()) {
                return true;
            }
        }

        return false;
    }

    private void registerHelpParams(@NotNull CommandTreeNode node,
                                    @NotNull CommandNode<T> commandNode,
                                    @NotNull SuggestionProvider<T> suggestionProvider,
                                    @NotNull Command<T> brigadierCommand) {
        // register a greedy argument at the end so the user can pass a page number, or partial command name to filter by

        RequiredArgumentBuilder<T, String> builder = RequiredArgumentBuilder
            .<T, String>argument("args", StringArgumentType.greedyString())
            .suggests(suggestionProvider)
            .requires(createPermissionPredicate(node))
            .executes(brigadierCommand);

        CommandNode<T> argument = builder.build();
        commandNode.addChild(argument);
    }

    private void registerParams(@NotNull BladeCommand command,
                                @NotNull CommandTreeNode node,
                                @NotNull CommandNode<T> commandNode,
                                @NotNull SuggestionProvider<T> suggestionProvider,
                                @NotNull Command<T> brigadierCommand,
                                boolean visibleOnly) {
        boolean hasGreedy = false;
        List<String> argumentPrefix = new ArrayList<>();

        for (DefinedArgument arg : command.arguments()) {
            argumentPrefix.add(arg.name());

            // Brigadier cannot represent sibling arguments with the same name but different parsers.
            // Conflicts are widened below, then Blade does final overload selection.
            CommandNode<T> existing = commandNode.getChild(arg.name());

            if (existing != null) {
                commandNode = existing;
                continue;
            }

            RequiredArgumentBuilder<T, ?> builder = RequiredArgumentBuilder
                .<T, Object>argument(arg.name(), mapBrigadierArgument(command, node, arg, argumentPrefix, visibleOnly))
                .suggests(suggestionProvider)
                .requires(createPermissionPredicate(node, new ArrayList<>(argumentPrefix), visibleOnly))
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

        if (!command.flags().isEmpty()) {
            // We add an argument to the end so the user can pass flags.
            // This is not a great solution, and we can't do it if there's already a greedy argument.
            // Not sure how to improve this right now.

            if (hasGreedy)
                return;

            if (commandNode.getChild("flags") != null)
                return;

            RequiredArgumentBuilder<T, String> builder = RequiredArgumentBuilder
                .<T, String>argument("flags", StringArgumentType.greedyString())
                .suggests(suggestionProvider)
                .requires(createFlagPermissionPredicate(node, new ArrayList<>(argumentPrefix), visibleOnly))
                .executes(brigadierCommand);

            CommandNode<T> argument = builder.build();
            commandNode.addChild(argument);
        }
    }

    @NotNull
    private List<BladeCommand> brigadierRegistrationOrder(@NotNull List<BladeCommand> commands) {
        List<BladeCommand> ordered = new ArrayList<>(commands);
        ordered.sort((a, b) -> {
            int max = Math.max(a.arguments().size(), b.arguments().size());

            for (int i = 0; i < max; i++) {
                int aScore = i < a.arguments().size()
                    ? brigadierPermissiveness(a.arguments().get(i))
                    : -1;
                int bScore = i < b.arguments().size()
                    ? brigadierPermissiveness(b.arguments().get(i))
                    : -1;

                if (aScore != bScore) {
                    return Integer.compare(bScore, aScore);
                }
            }

            return 0;
        });
        return ordered;
    }

    private int brigadierPermissiveness(@NotNull DefinedArgument argument) {
        Class<?> clazz = argument.type();

        if (clazz == String.class) {
            return argument.isGreedy() ? 1000 : 900;
        }

        if (clazz == double.class || clazz == Double.class) return 800;
        if (clazz == float.class || clazz == Float.class) return 700;
        if (clazz == long.class || clazz == Long.class) return 650;
        if (clazz == int.class || clazz == Integer.class) return 600;
        if (clazz == boolean.class || clazz == Boolean.class) return 500;

        return 900;
    }

    @NotNull
    private Predicate<T> createPermissionPredicate(@NotNull CommandTreeNode node) {
        return sender -> {
            Context context = createContext(sender);

            if (node.isLeaf()) {
                for (BladeCommand command : node.commands()) {
                    if (command.hasPermission(context)) {
                        return true;
                    }
                }

                return false;
            }

            if (node.isStub()) {
                return hasAccessibleCommand(node, context);
            }

            return true;
        };
    }

    @NotNull
    private Predicate<T> createPermissionPredicate(@NotNull CommandTreeNode node,
                                                   @NotNull List<String> argumentPrefix,
                                                   boolean visibleOnly) {
        return sender -> {
            Context context = createContext(sender);

            for (BladeCommand command : node.commands()) {
                if (visibleOnly && !command.shouldSendToClient()) continue;
                if (!argumentPrefixMatches(command, argumentPrefix, false)) continue;
                if (command.hasPermission(context)) return true;
            }

            return false;
        };
    }

    @NotNull
    private Predicate<T> createFlagPermissionPredicate(@NotNull CommandTreeNode node,
                                                       @NotNull List<String> argumentPrefix,
                                                       boolean visibleOnly) {
        return sender -> {
            Context context = createContext(sender);

            for (BladeCommand command : node.commands()) {
                if (visibleOnly && !command.shouldSendToClient()) continue;
                if (command.flags().isEmpty()) continue;
                if (!argumentPrefixMatches(command, argumentPrefix, true)) continue;
                if (command.hasPermission(context)) return true;
            }

            return false;
        };
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean argumentPrefixMatches(@NotNull BladeCommand command,
                                          @NotNull List<String> argumentPrefix,
                                          boolean exact) {
        if (exact && command.arguments().size() != argumentPrefix.size()) {
            return false;
        }

        if (!exact && command.arguments().size() < argumentPrefix.size()) {
            return false;
        }

        for (int i = 0; i < argumentPrefix.size(); i++) {
            if (!command.arguments().get(i).name().equals(argumentPrefix.get(i))) {
                return false;
            }
        }

        return true;
    }

    @NotNull
    private Predicate<T> createClientVisibilityPredicate(@NotNull CommandTreeNode node) {
        return sender -> hasVisibleAccessibleCommand(node, createContext(sender));
    }

    @NotNull
    private Context createContext(@NotNull T sender) {
        Sender<?> wrappedSender = wrapper.apply(converter.apply(sender));
        return new Context(blade, wrappedSender, "", new String[0]);
    }

    private boolean hasAccessibleCommand(@NotNull CommandTreeNode node,
                                         @NotNull Context context) {
        for (BladeCommand cmd : node.commands()) {
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

    private boolean hasVisibleCommand(@NotNull CommandTreeNode node) {
        for (BladeCommand cmd : node.commands()) {
            if (cmd.shouldSendToClient()) {
                return true;
            }
        }

        for (CommandTreeNode child : node.children().values()) {
            if (hasVisibleCommand(child)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasHiddenCommand(@NotNull CommandTreeNode node) {
        for (BladeCommand cmd : node.commands()) {
            if (!cmd.shouldSendToClient()) {
                return true;
            }
        }

        for (CommandTreeNode child : node.children().values()) {
            if (hasHiddenCommand(child)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasVisibleAccessibleCommand(@NotNull CommandTreeNode node,
                                                @NotNull Context context) {
        for (BladeCommand cmd : node.commands()) {
            if (cmd.shouldSendToClient() && cmd.hasPermission(context)) {
                return true;
            }
        }

        for (CommandTreeNode child : node.children().values()) {
            if (hasVisibleAccessibleCommand(child, context)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private ArgumentType<Object> mapBrigadierArgument(@NotNull BladeCommand command,
                                                      @NotNull CommandTreeNode node,
                                                      @NotNull DefinedArgument argument,
                                                      @NotNull List<String> argumentPrefix,
                                                      boolean visibleOnly) {
        if (hasConflictingBrigadierType(command, node, argumentPrefix, visibleOnly)) {
            ArgumentType<?> type = argument.isGreedy()
                ? StringArgumentType.greedyString()
                : StringArgumentType.string();

            return (ArgumentType<Object>) type;
        }

        return mapBrigadierArgument(command, argument);
    }

    private boolean hasConflictingBrigadierType(@NotNull BladeCommand command,
                                                @NotNull CommandTreeNode node,
                                                @NotNull List<String> argumentPrefix,
                                                boolean visibleOnly) {
        String expected = brigadierSignature(command, argumentPrefix.size() - 1);

        for (BladeCommand other : node.commands()) {
            if (visibleOnly && !other.shouldSendToClient()) continue;
            if (!argumentPrefixMatches(other, argumentPrefix, false)) continue;

            if (!expected.equals(brigadierSignature(other, argumentPrefix.size() - 1))) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    private String brigadierSignature(@NotNull BladeCommand command, int argumentIndex) {
        DefinedArgument argument = command.arguments().get(argumentIndex);
        Class<?> clazz = argument.type();

        String range = "";
        if (argument.hasRange()) {
            Range argRange = Objects.requireNonNull(argument.range());
            range = ":" + argRange.min() + ":" + argRange.max();
        }

        if (clazz == String.class) {
            if (argument.isGreedy()) return "string:greedy";
            if (command.parseQuotes() || argument.isQuoted()) return "string:quoted";
            return "string:word";
        }

        if (clazz == int.class || clazz == Integer.class) return "int" + range;
        if (clazz == long.class || clazz == Long.class) return "long" + range;
        if (clazz == float.class || clazz == Float.class) return "float" + range;
        if (clazz == double.class || clazz == Double.class) return "double" + range;
        if (clazz == boolean.class || clazz == Boolean.class) return "boolean";

        return "custom-string";
    }

    @NotNull
    private ArgumentType<Object> mapBrigadierArgument(@NotNull BladeCommand command,
                                                      @NotNull BladeParameter parameter) {
        Class<?> clazz = parameter.type();
        ArgumentType<?> type = StringArgumentType.string();

        if (clazz == String.class) {
            if (parameter.isGreedy()) type = StringArgumentType.greedyString();
            else if (command.parseQuotes() || parameter.isQuoted()) type = StringArgumentType.string();
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
