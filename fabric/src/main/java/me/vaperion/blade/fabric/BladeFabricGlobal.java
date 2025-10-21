package me.vaperion.blade.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.vaperion.blade.Blade;
import me.vaperion.blade.fabric.container.BladeFabricBrigadier;
import me.vaperion.blade.impl.node.ResolvedCommandNode;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BladeFabricGlobal implements DedicatedServerModInitializer {

    public static MinecraftServer SERVER;
    public static final List<Blade> ACTIVE_INSTANCES = new CopyOnWriteArrayList<>();

    @ApiStatus.Internal
    public static void triggerBrigadierSync() {
        if (SERVER == null)
            return;

        var manager = SERVER.getPlayerManager();

        for (var player : manager.getPlayerList()) {
            manager.sendCommandTree(player);
        }
    }

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SERVER = server;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            ACTIVE_INSTANCES.forEach(blade -> {
                registerCommands(blade, dispatcher, registry, env);
            });
        });
    }

    private void registerCommands(@NotNull Blade blade,
                                  @NotNull CommandDispatcher<ServerCommandSource> dispatcher,
                                  @NotNull CommandRegistryAccess registry,
                                  @NotNull CommandManager.RegistrationEnvironment env) {
        blade.labelToCommands().forEach((label, commands) -> {
            BladeFabricPlatform platform = blade.platformAs(BladeFabricPlatform.class);

            BladeFabricBrigadier brigadier = platform.brigadier();
            ResolvedCommandNode node = blade.nodeResolver().resolve(label);

            if (node == null)
                return;

            if (node.command() == null && node.subcommands().isEmpty())
                return;

            LiteralCommandNode<ServerCommandSource> literal = brigadier.getBuilder().buildLiteral(
                node,
                label,
                brigadier.delegatingSuggestionProvider(node),
                brigadier.delegatingExecutor(node)
            );

            dispatcher.getRoot().addChild(literal);
        });
    }
}
