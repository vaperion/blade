package me.vaperion.blade.impl;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.command.Permission;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.util.command.PermissionPredicate;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@RequiredArgsConstructor
public class PermissionTester {

    private final Blade blade;

    public boolean testPermission(@NotNull Context context, @NotNull BladeCommand command) {
        String permission = command.permission();

        if (permission == null || permission.isEmpty()) {
            // If the command doesn't have a permission, it's allowed
            return true;
        }

        if (permission.charAt(0) == Permission.PREDICATE_PREFIX) {
            // If the permission starts with @, it's a predicate

            String id = permission.substring(1);
            PermissionPredicate predicate = blade.permissionPredicates().get(id.toLowerCase(Locale.ROOT));

            if (predicate == null) {
                // If the predicate doesn't exist, fail
                blade.logger().warn("Command `" + command.mainLabel() + "` has a non-existent permission predicate: `" + id + "`!");
                return false;
            }

            return predicate.test(context, command);
        }

        // Let the sender implementation handle the permission check
        return context.sender().hasPermission(permission);
    }

}