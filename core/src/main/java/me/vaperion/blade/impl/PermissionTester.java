package me.vaperion.blade.impl;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
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

        if (permission == null || permission.isEmpty()) { // If the command doesn't have a permission, it's allowed
            return true;
        }

        if (permission.startsWith("@")) { // If the permission starts with @, it's a predicate
            String id = permission.substring(1);
            PermissionPredicate predicate = blade.permissionPredicates().get(id.toLowerCase(Locale.ROOT));

            if (predicate == null) { // If the predicate doesn't exist, it's allowed
                return true;
            }

            return predicate.test(context, command);
        }

        return context.sender().hasPermission(permission); // Let the sender implementation handle the permission check
    }

}