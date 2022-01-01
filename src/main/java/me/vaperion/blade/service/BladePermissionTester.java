package me.vaperion.blade.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.permissions.PermissionPredicate;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@RequiredArgsConstructor
public class BladePermissionTester {

    private final BladeCommandService commandService;

    public boolean testPermission(@NotNull BladeContext context, @NotNull BladeCommand command) {
        String permission = command.getPermission();

        if (permission == null || permission.isEmpty()) { // If the command doesn't have a permission, it's allowed
            return true;
        }

        if (permission.startsWith("@")) { // If the permission starts with @, it's a predicate
            String id = permission.substring(1);
            PermissionPredicate predicate = commandService.predicateMap.get(id.toLowerCase(Locale.ROOT));

            if (predicate == null) { // If the predicate doesn't exist, it's allowed
                return true;
            }

            return predicate.test(context, command);
        }

        return context.sender().hasPermission(permission); // Let the sender implementation handle the permission check
    }

}
