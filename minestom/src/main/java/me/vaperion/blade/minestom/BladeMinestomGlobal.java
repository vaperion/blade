package me.vaperion.blade.minestom;

import me.vaperion.blade.minestom.api.PermissionChecker;
import me.vaperion.blade.minestom.context.MinestomSender;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class BladeMinestomGlobal {

    private static volatile MinecraftServer SERVER;
    private static volatile PermissionChecker PERMISSION_CHECKER;

    @ApiStatus.Internal
    public static void setServer(@NotNull MinecraftServer server) {
        SERVER = server;
    }

    @ApiStatus.Internal
    public static void setPermissionChecker(@NotNull PermissionChecker checker) {
        PERMISSION_CHECKER = checker;
    }

    @NotNull
    public static MinecraftServer server() {
        if (SERVER == null) {
            throw new IllegalStateException("MinecraftServer has not been set!");
        }

        return SERVER;
    }

    public static boolean hasPermission(@NotNull MinestomSender sender,
                                        @NotNull String permissionNode) {
        if (PERMISSION_CHECKER == null) {
            throw new IllegalStateException("PermissionChecker has not been set!");
        }

        return PERMISSION_CHECKER.hasPermission(sender, permissionNode);
    }

}
