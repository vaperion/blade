package me.vaperion.blade.fabric.context;

import lombok.RequiredArgsConstructor;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Sender;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public final class FabricSender implements Sender<ServerCommandSource> {
    private final ServerCommandSource commandSource;

    @NotNull
    @Override
    public ServerCommandSource rawSender() {
        return commandSource;
    }

    @Override
    public @NotNull Object underlyingSender() {
        var entity = commandSource.getEntity();
        return entity != null ? entity : commandSource;
    }

    @Override
    public @NotNull Class<?> underlyingSenderType() {
        return commandSource.getEntity() != null
            ? commandSource.getEntity().getClass()
            : ServerCommandSource.class;
    }

    @NotNull
    @Override
    public String name() {
        return commandSource.getName();
    }

    @Override
    public boolean hasPermission(@NotNull String permissionNode) {
        boolean isConsole = commandSource.output instanceof MinecraftDedicatedServer;

        if ("op".equals(permissionNode))
            return isConsole || commandSource.hasPermissionLevel(4);
        if ("console".equals(permissionNode))
            return isConsole;

        return Permissions.check(commandSource, permissionNode);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T parseAs(@NotNull Class<T> clazz) {
        // We do exact comparisons instead of isAssignableFrom / others here on purpose
        var entity = commandSource.getEntity();

        if (clazz.equals(ServerPlayerEntity.class) && entity instanceof ServerPlayerEntity)
            return (T) entity;
        else if (clazz.equals(Entity.class) && entity != null)
            return (T) entity;
        else if (clazz.equals(ServerCommandSource.class) || clazz.equals(CommandSource.class))
            return (T) commandSource;

        return null;
    }

    @Override
    public boolean isExpectedType(@NotNull BladeCommand command) {
        return parseAs(command.senderType()) != null;
    }
}