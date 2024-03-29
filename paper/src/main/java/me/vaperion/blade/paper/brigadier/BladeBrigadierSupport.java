package me.vaperion.blade.paper.brigadier;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.argument.Range;
import me.vaperion.blade.command.Parameter;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.context.WrappedSender;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public final class BladeBrigadierSupport implements Listener {

    private final Blade blade;
    private final BladeNodeDiscovery nodeDiscovery;
    private final Function<CommandSender, WrappedSender<?>> wrappedSenderFunction;

    public BladeBrigadierSupport(@NotNull Blade blade,
                                 @NotNull Function<CommandSender, WrappedSender<?>> wrappedSenderFunction) throws ClassNotFoundException {
        Class.forName("com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent");

        this.blade = blade;
        this.nodeDiscovery = new BladeNodeDiscovery(blade);
        this.wrappedSenderFunction = wrappedSenderFunction;

        Bukkit.getPluginManager().registerEvents(this, (Plugin) blade.getPlatform().getPluginInstance());
    }

    @EventHandler
    public void onCommandRegistered(CommandRegisteredEvent<BukkitBrigadierCommandSource> event) {
        SimpleBladeNode node = nodeDiscovery.discoverCommand(event.getCommandLabel());
        if (node == null) return;

        event.setLiteral(buildLiteral(
              node,
              event.getCommandLabel(),
              event.getBrigadierCommand(),
              event.getBrigadierCommand()
        ));
    }

    @NotNull
    private LiteralCommandNode<BukkitBrigadierCommandSource> buildLiteral(
          @NotNull SimpleBladeNode node,
          @NotNull String label,
          @NotNull SuggestionProvider<BukkitBrigadierCommandSource> suggestionProvider,
          @NotNull Command<BukkitBrigadierCommandSource> brigadierCommand) {
        LiteralArgumentBuilder<BukkitBrigadierCommandSource> builder = LiteralArgumentBuilder.<BukkitBrigadierCommandSource>literal(label)
              .requires(createPermissionPredicate(node))
              .executes(brigadierCommand);

        LiteralCommandNode<BukkitBrigadierCommandSource> root = builder.build();

        registerParams(node, root, suggestionProvider, brigadierCommand);

        for (SimpleBladeNode subCommand : node.getSubCommands()) {
            if (subCommand.isStub()) continue;

            String subLabel = subCommand.getCommand().getUsageAlias();
            String[] parts = subLabel.split(" ");
            String rest = subLabel.substring(parts[0].length() + 1);

            registerSubCommand(subCommand, rest, suggestionProvider, brigadierCommand, root);
        }

        return root;
    }

    private void registerSubCommand(@NotNull SimpleBladeNode subCommand,
                                    @NotNull String label,
                                    @NotNull SuggestionProvider<BukkitBrigadierCommandSource> suggestionProvider,
                                    @NotNull Command<BukkitBrigadierCommandSource> brigadierCommand,
                                    @NotNull LiteralCommandNode<BukkitBrigadierCommandSource> root) {
        if (subCommand.isStub()) return;

        if (label.contains(" ")) {
            String[] parts = label.split(" ");

            String stubName = parts[0];
            String rest = label.substring(stubName.length() + 1);

            CommandNode<BukkitBrigadierCommandSource> subCommandNode = root.getChild(stubName);

            if (subCommandNode == null) {
                subCommandNode = LiteralArgumentBuilder.<BukkitBrigadierCommandSource>literal(stubName)
                      .requires(createPermissionPredicate(subCommand))
                      .executes(brigadierCommand)
                      .build();
            }

            root.addChild(subCommandNode);
            registerSubCommand(subCommand, rest, suggestionProvider, brigadierCommand, (LiteralCommandNode<BukkitBrigadierCommandSource>) subCommandNode);
        } else {
            CommandNode<BukkitBrigadierCommandSource> subCommandNode = root.getChild(label);

            if (subCommandNode == null) {
                subCommandNode = LiteralArgumentBuilder.<BukkitBrigadierCommandSource>literal(label)
                      .requires(createPermissionPredicate(subCommand))
                      .executes(brigadierCommand)
                      .build();
            }

            root.addChild(subCommandNode);
            registerParams(subCommand, subCommandNode, suggestionProvider, brigadierCommand);
        }
    }

    private void registerParams(@NotNull SimpleBladeNode node,
                                @NotNull CommandNode<BukkitBrigadierCommandSource> commandNode,
                                @NotNull SuggestionProvider<BukkitBrigadierCommandSource> suggestionProvider,
                                @NotNull Command<BukkitBrigadierCommandSource> brigadierCommand) {
        if (node.isStub()) return;

        for (Parameter.CommandParameter parameter : node.getCommand().getCommandParameters()) {
            RequiredArgumentBuilder<BukkitBrigadierCommandSource, Object> builder = RequiredArgumentBuilder
                  .<BukkitBrigadierCommandSource, Object>argument(parameter.getName(), mapBrigadierArgument(parameter))
                  .suggests(suggestionProvider)
                  .requires(createPermissionPredicate(node))
                  .executes(brigadierCommand);

            CommandNode<BukkitBrigadierCommandSource> argument = builder.build();
            commandNode.addChild(argument);
            commandNode = argument;
        }
    }

    // This is a bit weird. Brigadier on newer versions literally HIDES commands if you don't have permissions / enter invalid args
    //  so instead of seeing the usage, you see unknown command. To fix this, we just tell brigadier that everyone has permission
    //  to execute the command, which then properly delegates to Blade's handler.
    @NotNull
    private Predicate<BukkitBrigadierCommandSource> createPermissionPredicate(@NotNull SimpleBladeNode node) {
        return sender -> {
            WrappedSender<?> wrappedSender = wrappedSenderFunction.apply(sender.getBukkitSender());
            Context context = new Context(blade, wrappedSender, "", new String[0]);

            if (node.getCommand() != null && node.getCommand().isHidden()) {
                boolean result = blade.getPermissionTester().testPermission(context, node.getCommand());
                if (!result) return false;
            }

            for (SimpleBladeNode subCommand : node.getSubCommands()) {
                if (!subCommand.getCommand().isHidden()) continue;

                boolean result = blade.getPermissionTester().testPermission(context, subCommand.getCommand());
                if (!result) return false;
            }

            return true;
        };
    }

    @NotNull
    private ArgumentType<Object> mapBrigadierArgument(@NotNull Parameter parameter) {
        Class<?> clazz = parameter.getType();
        ArgumentType<?> type = StringArgumentType.string();

        if (clazz == String.class) {
            if (parameter.isText()) type = StringArgumentType.greedyString();
            else type = StringArgumentType.string();
        }

        if (clazz == int.class || clazz == Integer.class) {
            if (parameter.hasRange()) {
                Range range = parameter.getRange();
                type = IntegerArgumentType.integer((int) Math.floor(range.min()), (int) Math.ceil(range.max()));
            } else type = IntegerArgumentType.integer();
        }

        if (clazz == float.class || clazz == Float.class) {
            if (parameter.hasRange()) {
                Range range = parameter.getRange();
                type = FloatArgumentType.floatArg((float) range.min(), (float) range.max());
            } else type = FloatArgumentType.floatArg();
        }

        if (clazz == double.class || clazz == Double.class) {
            if (parameter.hasRange()) {
                Range range = parameter.getRange();
                type = DoubleArgumentType.doubleArg(range.min(), range.max());
            } else type = DoubleArgumentType.doubleArg();
        }

        if (clazz == long.class || clazz == Long.class) {
            if (parameter.hasRange()) {
                Range range = parameter.getRange();
                type = LongArgumentType.longArg((long) Math.floor(range.min()), (long) Math.ceil(range.max()));
            } else type = LongArgumentType.longArg();
        }

        if (clazz == boolean.class || clazz == Boolean.class)
            type = BoolArgumentType.bool();

        //noinspection unchecked
        return (ArgumentType<Object>) type;
    }
}
