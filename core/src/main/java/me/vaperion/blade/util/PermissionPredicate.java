package me.vaperion.blade.util;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface PermissionPredicate extends BiPredicate<Context, BladeCommand> {
    boolean test(@NotNull Context context, @NotNull BladeCommand command);
}