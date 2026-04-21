package me.vaperion.blade.paper.brigadier;

import com.mojang.brigadier.context.CommandContext;
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
import me.vaperion.blade.brigadier.BrigadierRichSuggestionsBuilder;
import me.vaperion.blade.bukkit.container.BukkitContainer;
import me.vaperion.blade.bukkit.context.BukkitSender;
import me.vaperion.blade.paper.BladePaperPlatform;
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
                container.tabComplete(sender(ctx), input(ctx), new BrigadierRichSuggestionsBuilder(builder)),
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
        blade.commandTree().roots().forEach((label, node) -> {
            LiteralCommandNode<CommandSourceStack> literal = builder.buildLiteral(
                node,
                label,
                delegate.suggestionProvider(node),
                delegate.executor(node)
            );

            if (registerModern(registrar, literal)) {
                // try registering using the modern method first, this also allows setting the namespace
                return;
            }

            if (registerLegacy(registrar, literal)) {
                // fallback to old method, only allows passing custom flags
                return;
            }

            // if both methods fail, just use the default register method
            registrar.register(literal);
        });
    }

    private static volatile Class<?> PAPER_COMMANDS;
    private static volatile Method REGISTER_INTERNAL;

    private static boolean MODERN_SUPPORTED = true;
    private static boolean LEGACY_SUPPORTED = true;

    private boolean registerModern(@NotNull Commands registrar,
                                   @NotNull LiteralCommandNode<CommandSourceStack> literal) {
        if (!MODERN_SUPPORTED) {
            return false;
        }

        try {
            PluginMeta meta = blade.platformAs(BladePaperPlatform.class).plugin().getPluginMeta();

            String namespace = blade.configuration().useCommandNameAsQualifier()
                ? literal.getLiteral().toLowerCase(Locale.ROOT)
                : blade.configuration().commandQualifier().toLowerCase(Locale.ROOT);

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

            return true;
        } catch (Throwable ignored) {
            MODERN_SUPPORTED = false;

            blade.logger().warn("Failed to register brigadier command in custom namespace. This is likely due to an incompatible Paper version.");

            return false;
        }
    }

    private boolean registerLegacy(@NotNull Commands registrar,
                                   @NotNull LiteralCommandNode<CommandSourceStack> literal) {
        if (!LEGACY_SUPPORTED) {
            return false;
        }

        try {
            PluginMeta meta = blade.platformAs(BladePaperPlatform.class).plugin().getPluginMeta();

            registrar.registerWithFlags(
                meta,
                literal,
                null,
                List.of(),
                Set.of(CommandRegistrationFlag.FLATTEN_ALIASES)
            );

            return true;
        } catch (Throwable ignored) {
            LEGACY_SUPPORTED = false;

            blade.logger().warn("Failed to register brigadier command using fallback method. This is likely due to an incompatible Paper version.");

            return false;
        }
    }
}
