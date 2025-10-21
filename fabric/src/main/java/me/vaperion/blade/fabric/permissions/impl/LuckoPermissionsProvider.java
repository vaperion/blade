package me.vaperion.blade.fabric.permissions.impl;

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.vaperion.blade.fabric.permissions.PermissionsProvider;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

public final class LuckoPermissionsProvider implements PermissionsProvider {

    @Override
    public boolean hasPermission(@NotNull ServerCommandSource source,
                                 @NotNull String permission) {
        return Permissions.check(source, permission);
    }
}
