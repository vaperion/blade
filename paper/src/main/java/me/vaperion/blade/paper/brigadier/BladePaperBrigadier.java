package me.vaperion.blade.paper.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandRegistrationFlag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.brigadier.BladeBrigadierBuilder;
import me.vaperion.blade.brigadier.BladeBrigadierDelegate;
import me.vaperion.blade.brigadier.BrigadierCompat;
import me.vaperion.blade.brigadier.BrigadierRichSuggestionsBuilder;
import me.vaperion.blade.bukkit.container.BukkitContainer;
import me.vaperion.blade.bukkit.context.BukkitSender;
import me.vaperion.blade.paper.BladePaperPlatform;
import me.vaperion.blade.tree.CommandTreeNode;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
            (ctx, builder, container) ->
                container.tabComplete(sender(ctx), input(builder), new BrigadierRichSuggestionsBuilder(builder)),
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
        return input(ctx.getInput());
    }

    @NotNull
    private String input(@NotNull SuggestionsBuilder builder) {
        return input(builder.getInput());
    }

    @NotNull
    private String input(@NotNull String input) {
        if (input.startsWith("/"))
            input = input.substring(1);

        return removeCommandQualifier(input);
    }

    private void registerCommands(@NotNull Commands registrar) {
        blade.commandTree().roots().forEach((label, node) -> {
            LiteralCommandNode<CommandSourceStack> literal = builder.buildLiteral(
                node,
                label,
                delegate.suggestionProvider(node),
                delegate.executor(node)
            );

            String commandNamespace = namespace(node, literal);

            if (registerModern(registrar, node, literal, commandNamespace)) {
                // try registering using the modern method first, this also allows setting the namespace
                return;
            }

            if (registerLegacy(registrar, node, literal, commandNamespace)) {
                // fallback to old method, only allows passing custom flags
                return;
            }

            // if both methods fail, just use the default register method
            registrar.register(literal);
            syncRegisteredClientNodes(registrar, commandNamespace, node, literal);
        });
    }

    @NotNull
    private PluginMeta pluginMeta() {
        return blade.platformAs(BladePaperPlatform.class).plugin().getPluginMeta();
    }

    @NotNull
    private String pluginNamespace() {
        return pluginMeta().getName().toLowerCase(Locale.ROOT);
    }

    private static volatile Class<?> PAPER_COMMANDS;
    private static volatile Method REGISTER_INTERNAL;

    private static boolean MODERN_SUPPORTED = true;
    private static boolean LEGACY_SUPPORTED = true;

    private boolean registerModern(@NotNull Commands registrar,
                                   @NotNull CommandTreeNode node,
                                   @NotNull LiteralCommandNode<CommandSourceStack> literal,
                                   @NotNull String namespace) {
        if (!MODERN_SUPPORTED) {
            return false;
        }

        try {
            PluginMeta meta = pluginMeta();

            if (namespace.equals(meta.getName().toLowerCase(Locale.ROOT))) {
                // no point in using reflection as the default implementation will work just fine in this case
                return false;
            }

            if (PAPER_COMMANDS == null) {
                PAPER_COMMANDS = Class.forName("io.papermc.paper.command.brigadier.PaperCommands");

                REGISTER_INTERNAL = PAPER_COMMANDS.getDeclaredMethod(
                    "registerWithFlagsInternal",

                    PluginMeta.class,
                    String.class,
                    String.class,
                    LiteralCommandNode.class,
                    String.class,
                    Collection.class,
                    Set.class);

                REGISTER_INTERNAL.setAccessible(true);
            }

            REGISTER_INTERNAL.invoke(
                PAPER_COMMANDS.cast(registrar),

                /* pluginMeta */ meta,
                /* namespace */ namespace,
                /* helpNamespaceOverride */ null,
                /* node */ literal,
                /* description */ null,
                /* aliases */ List.of(),
                /* flags */ Set.of(CommandRegistrationFlag.FLATTEN_ALIASES)
            );

            syncRegisteredClientNodes(registrar, namespace, node, literal);

            return true;
        } catch (Throwable ignored) {
            MODERN_SUPPORTED = false;

            blade.logger().warn("Failed to register brigadier command in custom namespace. This is likely due to an incompatible Paper version.");

            return false;
        }
    }

    @NotNull
    private String namespace(@NotNull CommandTreeNode node,
                             @NotNull LiteralCommandNode<CommandSourceStack> literal) {
        if (blade.configuration().useCommandNameAsQualifier()) {
            return literal.getLiteral().toLowerCase(Locale.ROOT);
        }

        if (node.container() instanceof BukkitContainer container) {
            return container.registrationQualifier().toLowerCase(Locale.ROOT);
        }

        return blade.configuration().commandQualifier().toLowerCase(Locale.ROOT);
    }

    private boolean registerLegacy(@NotNull Commands registrar,
                                   @NotNull CommandTreeNode node,
                                   @NotNull LiteralCommandNode<CommandSourceStack> literal,
                                   @NotNull String namespace) {
        if (!LEGACY_SUPPORTED) {
            return false;
        }

        try {
            PluginMeta meta = pluginMeta();

            registrar.registerWithFlags(
                meta,
                literal,
                null,
                List.of(),
                Set.of(CommandRegistrationFlag.FLATTEN_ALIASES)
            );

            syncRegisteredClientNodes(registrar, namespace, node, literal);

            return true;
        } catch (Throwable ignored) {
            LEGACY_SUPPORTED = false;

            blade.logger().warn("Failed to register brigadier command using fallback method. This is likely due to an incompatible Paper version.");

            return false;
        }
    }

    private void syncRegisteredClientNodes(@NotNull Commands registrar,
                                           @NotNull String namespace,
                                           @NotNull CommandTreeNode treeNode,
                                           @NotNull LiteralCommandNode<CommandSourceStack> literal) {
        CommandNode<CommandSourceStack> clientNode = BrigadierCompat.getClientNode(literal);
        LiteralCommandNode<CommandSourceStack> clientLiteral = hasHiddenCommand(treeNode) && clientNode instanceof LiteralCommandNode<CommandSourceStack> customClientLiteral
            ? customClientLiteral
            : literal;

        syncRegisteredClientNode(literal,
            copyLiteral(literal.getLiteral(), clientLiteral));

        CommandNode<CommandSourceStack> registeredPlain = registrar.getDispatcher()
            .getRoot()
            .getChild(literal.getLiteral());

        if (registeredPlain != null && registeredPlain != literal) {
            syncRegisteredClientNode(registeredPlain,
                copyLiteral(literal.getLiteral(), clientLiteral));
        }

        CommandNode<CommandSourceStack> registeredNamespaced = registrar.getDispatcher()
            .getRoot()
            .getChild(namespace + ":" + literal.getLiteral());

        if (registeredNamespaced != null) {
            syncRegisteredClientNode(registeredNamespaced,
                copyLiteral(namespace + ":" + literal.getLiteral(), clientLiteral));
        }
    }

    private boolean hasHiddenCommand(@NotNull CommandTreeNode node) {
        return node.commands().stream().anyMatch(command -> !command.shouldSendToClient()) ||
            node.children().values().stream().anyMatch(this::hasHiddenCommand);
    }

    private void syncRegisteredClientNode(@NotNull CommandNode<CommandSourceStack> node,
                                          @NotNull LiteralCommandNode<CommandSourceStack> clientNode) {
        BrigadierCompat.setClientNode(node, clientNode);

        CommandNode<CommandSourceStack> unwrapped = BrigadierCompat.getUnwrappedCached(node);
        if (unwrapped != null) {
            BrigadierCompat.setClientNode(unwrapped, clientNode);
        }
    }

    @NotNull
    private LiteralCommandNode<CommandSourceStack> copyLiteral(@NotNull String newLiteral,
                                                               @NotNull LiteralCommandNode<CommandSourceStack> source) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder
            .<CommandSourceStack>literal(newLiteral)
            .requires(source.getRequirement())
            .forward(source.getRedirect(), source.getRedirectModifier(), source.isFork());

        if (source.getCommand() != null) {
            builder.executes(source.getCommand());
        }

        for (CommandNode<CommandSourceStack> child : source.getChildren()) {
            builder.then(child);
        }

        return builder.build();
    }
}
