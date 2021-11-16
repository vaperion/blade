package me.vaperion.blade.command.argument;

import me.vaperion.blade.command.context.BladeContext;
import me.vaperion.blade.command.exception.BladeExitMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@FunctionalInterface
public interface BladeProvider<T> {

    @Nullable
    T provide(@NotNull BladeContext context, @NotNull BladeArgument argument) throws BladeExitMessage;

    @NotNull
    default List<String> suggest(@NotNull BladeContext context, @NotNull BladeArgument argument) throws BladeExitMessage {
        return Collections.emptyList();
    }

}
