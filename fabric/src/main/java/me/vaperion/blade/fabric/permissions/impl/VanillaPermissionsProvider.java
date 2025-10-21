package me.vaperion.blade.fabric.permissions.impl;

import me.vaperion.blade.fabric.BladeFabricGlobal;
import me.vaperion.blade.fabric.permissions.PermissionsProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

public final class VanillaPermissionsProvider implements PermissionsProvider {

    @Override
    public boolean hasPermission(@NotNull ServerCommandSource source,
                                 @NotNull String permission) {
        if (source.output instanceof MinecraftDedicatedServer) {
            // Console has all permissions
            return true;
        }

        if (source.getEntity() instanceof ServerPlayerEntity player) {
            // Allow ops
            return BladeFabricGlobal.SERVER.getPlayerManager().isOperator(player.getPlayerConfigEntry());
        }

        return false;
    }
}
