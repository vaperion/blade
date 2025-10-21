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
@SuppressWarnings("unused")
@FunctionalInterface
public interface SenderProvider<T> {
    /**
     * Converts the given {@link Sender} into the specific type.
     *
     * @param context the command context
     * @param sender  the sender
     * @return the converted value, or null if conversion failed
     *
     * @throws BladeParseError if an error occurs during conversion (use {@link BladeParseError#fatal(String)} or {@link BladeParseError#recoverable(String)}.)
     */
    @Nullable T provide(@NotNull Context context, @NotNull Sender<?> sender) throws BladeParseError;

    /**
     * Provides a friendly name for this sender type, used in error messages.
     *
     * @param plural whether the name should be plural
     * @return the friendly name, or null to use a default name
     */
    @Nullable
    default String friendlyName(boolean plural) {
        return null;
    }
}
