package me.vaperion.blade.argument;

import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * A provider that converts the given {@link Argument} into a specific type.
 *
 * @param <T> the type
 */
@FunctionalInterface
public interface ArgumentProvider<T> {
    @Nullable T provide(@NotNull Context context, @NotNull Argument argument) throws BladeExitMessage;

    @NotNull
    default List<String> suggest(@NotNull Context context, @NotNull Argument argument) throws BladeExitMessage {
        return Collections.emptyList();
    }
}
