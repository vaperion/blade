package me.vaperion.blade.fabric.permissions.impl;

import me.vaperion.blade.fabric.BladeFabricGlobal;
import me.vaperion.blade.fabric.permissions.PermissionsProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class VanillaPermissionsProvider implements PermissionsProvider {

    @Override
    public boolean hasPermission(@NotNull CommandSourceStack source,
                                 @NotNull String permission) {
        if (source.hasPermission(4)) {
            // Permission level 4 usually means the player has all permissions
            return true;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            // Allow ops
            return BladeFabricGlobal.server().getPlayerList().isOp(player.nameAndId());
        }

        return false;
    }
}
