package me.vaperion.blade.permissions;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.BladeContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface PermissionPredicate extends BiPredicate<BladeContext, BladeCommand> {
    boolean test(@NotNull BladeContext context, @NotNull BladeCommand command);
}
