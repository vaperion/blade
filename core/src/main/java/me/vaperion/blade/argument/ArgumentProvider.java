package me.vaperion.blade.argument;

import me.vaperion.blade.annotation.parameter.Opt;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A provider that converts the given {@link InputArgument} into a specific type.
 *
 * @param <T> the type
 */
@SuppressWarnings("unused")
@FunctionalInterface
public interface ArgumentProvider<T> {
    /**
     * Whether this provider should be called when the argument is null.
     * <p>
     * This is useful for providers that want to implement logic for optional arguments,
     * like handling {@link Opt.Type#SENDER}.
     *
     * @return true if the provider should be called with a null argument, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean handlesNullInputArguments() {
        return false;
    }

    @Nullable
    T provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError;

    default void suggest(@NotNull Context ctx,
                         @NotNull InputArgument arg,
                         @NotNull SuggestionsBuilder suggestions) throws BladeParseError {
    }
}
