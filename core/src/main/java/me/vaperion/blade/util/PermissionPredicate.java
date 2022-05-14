package me.vaperion.blade.util;

import me.vaperion.blade.command.Command;
import me.vaperion.blade.context.Context;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface PermissionPredicate extends BiPredicate<Context, Command> {
    boolean test(@NotNull Context context, @NotNull Command command);
}