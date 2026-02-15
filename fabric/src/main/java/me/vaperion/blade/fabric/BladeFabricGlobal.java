package me.vaperion.blade.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BladeFabricGlobal implements DedicatedServerModInitializer {

    public static final List<Blade> ACTIVE_INSTANCES = new CopyOnWriteArrayList<>();

    private static final Set<String> REGISTERED_ROOT_LABELS = ConcurrentHashMap.newKeySet();

    private static final Field CHILDREN_FIELD = resolveNodeMapField("children");
    private static final Field LITERALS_FIELD = resolveNodeMapField("literals");
    private static final Field ARGUMENTS_FIELD = resolveNodeMapField("arguments");

    private static volatile BladeFabricGlobal GLOBAL;
    private static volatile MinecraftServer SERVER;

    @ApiStatus.Internal
    public static void triggerBrigadierSync() {
        if (SERVER == null)
            return;

        if (ACTIVE_INSTANCES.isEmpty())
            return;

        try {
            var commands = SERVER.getCommandManager();
            var registries = SERVER.getRegistryManager();
            var root = commands.getDispatcher().getRoot();

            // unregister all old blade command nodes
            for (String label : REGISTERED_ROOT_LABELS) {
                GLOBAL.unregisterCommandLabel(root, label);
            }

            GLOBAL.registerAllCommands(
                commands.getDispatcher(),
                CommandManager.createRegistryAccess(registries),
                CommandManager.RegistrationEnvironment.DEDICATED
            );

            REGISTERED_ROOT_LABELS.clear();

            ACTIVE_INSTANCES.forEach(blade -> REGISTERED_ROOT_LABELS.addAll(
                blade.commandTree().roots().keySet()
            ));
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

            // unregister conflicting command nodes
            // (e.g. if someone makes a /kick command with blade, we need to unregister the vanilla /kick)
            unregisterCommandLabel(dispatcher.getRoot(), label);

            dispatcher.getRoot().addChild(literal);
        });
    }

    private void unregisterCommandLabel(@NotNull RootCommandNode<ServerCommandSource> root,
                                        @NotNull String label) {
        removeNodeFromMap(CHILDREN_FIELD, root, label);
        removeNodeFromMap(LITERALS_FIELD, root, label);
        removeNodeFromMap(ARGUMENTS_FIELD, root, label);
    }

    private static void removeNodeFromMap(@NotNull Field field,
                                          @NotNull RootCommandNode<ServerCommandSource> root,
                                          @NotNull String label) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, ?> nodes = (Map<String, ?>) field.get(root);

            if (nodes != null) {
                nodes.remove(label);
            }
        } catch (Throwable t) {
            if (!ACTIVE_INSTANCES.isEmpty()) {
                ACTIVE_INSTANCES.getFirst().logger().error(
                    t,
                    "Failed to unregister Brigadier command node `%s` via field `%s`.",
                    label,
                    field.getName()
                );
            }
        }
    }

    @NotNull
    private static Field resolveNodeMapField(@NotNull String fieldName) {
        try {
            Field field = CommandNode.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to resolve Brigadier CommandNode field: " + fieldName, t);
        }
    }

    @NotNull
    public static MinecraftServer server() {
        if (SERVER == null) {
            throw new IllegalStateException("MinecraftServer has not been set!");
        }

        return SERVER;
    }
}
