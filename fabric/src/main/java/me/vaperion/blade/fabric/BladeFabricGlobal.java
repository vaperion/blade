package me.vaperion.blade.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.vaperion.blade.Blade;
import me.vaperion.blade.fabric.container.BladeFabricBrigadier;
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

    public static final List<Blade> ACTIVE_INSTANCES = new CopyOnWriteArrayList<>();

    private static BladeFabricGlobal GLOBAL;
    public static MinecraftServer SERVER;

    @ApiStatus.Internal
    public static void triggerBrigadierSync() {
        if (SERVER == null)
            return;

        if (ACTIVE_INSTANCES.isEmpty())
            return;

        try {
            var commands = SERVER.getCommandManager();
            var registries = SERVER.getRegistryManager();

            GLOBAL.registerAllCommands(
                commands.getDispatcher(),
                CommandManager.createRegistryAccess(registries),
                CommandManager.RegistrationEnvironment.DEDICATED
            );
        } catch (Throwable t) {
            // Just choose the first instance to log the error

            ACTIVE_INSTANCES.getFirst().logger()
                .error(t, "Failed to register commands during brigadier sync!");
        }

        var manager = SERVER.getPlayerManager();

        for (var player : manager.getPlayerList()) {
            manager.sendCommandTree(player);
        }
    }

    @Override
    public void onInitializeServer() {
        GLOBAL = this;

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SERVER = server;
        });

        CommandRegistrationCallback.EVENT.register(this::registerAllCommands);
    }

    private void registerAllCommands(@NotNull CommandDispatcher<ServerCommandSource> dispatcher,
                                     @NotNull CommandRegistryAccess registry,
                                     @NotNull CommandManager.RegistrationEnvironment env) {
        ACTIVE_INSTANCES.forEach(blade -> {
            registerCommands(blade, dispatcher, registry, env);
        });
    }

    private void registerCommands(@NotNull Blade blade,
                                  @NotNull CommandDispatcher<ServerCommandSource> dispatcher,
                                  @NotNull CommandRegistryAccess registry,
                                  @NotNull CommandManager.RegistrationEnvironment env) {
        blade.commandTree().roots().forEach((label, node) -> {
            BladeFabricPlatform platform = blade.platformAs(BladeFabricPlatform.class);
            BladeFabricBrigadier brigadier = platform.brigadier();

            LiteralCommandNode<ServerCommandSource> literal = brigadier.builder().buildLiteral(
                node,
                label,
                brigadier.delegate().suggestionProvider(node),
                brigadier.delegate().executor(node)
            );

            dispatcher.getRoot().addChild(literal);
        });
    }
}
