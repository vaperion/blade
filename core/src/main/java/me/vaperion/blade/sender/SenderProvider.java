package me.vaperion.blade.sender;

import me.vaperion.blade.context.Context;
import me.vaperion.blade.context.Sender;
import me.vaperion.blade.exception.BladeParseError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A provider that converts the given {@link Sender} into a specific type.
 *
 * @param <T> the type
 */
@FunctionalInterface
public interface SenderProvider<T> {
    @Nullable T provide(@NotNull Context context, @NotNull Sender<?> sender) throws BladeParseError;
}
