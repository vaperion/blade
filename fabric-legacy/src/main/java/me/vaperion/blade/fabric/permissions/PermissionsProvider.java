package me.vaperion.blade.fabric.permissions;

import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

public interface PermissionsProvider {

    /**
     * Checks if the given source has the specified permission.
     *
     * @param source     the command source
     * @param permission the permission node to check
     * @return true if the source has the permission, false otherwise
     */
    boolean hasPermission(@NotNull CommandSourceStack source,
                          @NotNull String permission);

}
