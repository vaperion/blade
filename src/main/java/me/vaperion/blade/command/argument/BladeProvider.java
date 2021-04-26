package me.vaperion.blade.command.argument;

import me.vaperion.blade.command.container.BladeParameter;
import me.vaperion.blade.command.context.BladeContext;
import me.vaperion.blade.command.exception.BladeExitMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@FunctionalInterface
public interface BladeProvider<T> {

    @Nullable
    T provide(@NotNull BladeContext context, @NotNull BladeParameter parameter, @Nullable String input) throws BladeExitMessage;

    @NotNull
    default List<String> suggest(@NotNull BladeContext context, @NotNull String input) throws BladeExitMessage {
        return Collections.emptyList();
    }

}
