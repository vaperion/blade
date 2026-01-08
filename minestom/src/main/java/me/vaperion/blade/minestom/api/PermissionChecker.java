package me.vaperion.blade.minestom.api;

import me.vaperion.blade.minestom.context.MinestomSender;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface PermissionChecker {

    /**
     * Checks if the sender has the specified permission node.
     *
     * @param sender         the sender to check
     * @param permissionNode the permission node to check
     * @return true if the sender has the permission node, false otherwise
     */
    boolean hasPermission(@NotNull MinestomSender sender, @NotNull String permissionNode);

}
