package me.vaperion.blade.argument;

import me.vaperion.blade.annotation.command.Quoted;
import me.vaperion.blade.annotation.parameter.Name;
import me.vaperion.blade.annotation.parameter.Opt;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;

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

    /**
     * Converts the given {@link InputArgument} into the specific type.
     *
     * @param ctx the command context
     * @param arg the input argument
     * @return the converted value, or null if conversion failed
     *
     * @throws BladeParseError if an error occurs during conversion (use {@link BladeParseError#fatal(String)} or {@link BladeParseError#recoverable(String)}.)
     */
    @Nullable
    T provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError;

    /**
     * Suggests possible completions for the given {@link InputArgument}.
     *
     * @param ctx         the command context
     * @param arg         the input argument
     * @param suggestions the suggestions builder
     * @throws BladeParseError if an error occurs during suggestion generation (use {@link BladeParseError#fatal(String)} or {@link BladeParseError#recoverable(String)}.)
     */
    default void suggest(@NotNull Context ctx,
                         @NotNull InputArgument arg,
                         @NotNull SuggestionsBuilder suggestions) throws BladeParseError {
    }

    /**
     * Provides a default argument name for this provider.
     *
     * @param element the annotated element (parameter)
     * @return the default argument name, or null if none
     *
     * @see Name
     */
    @Nullable
    default String defaultArgName(@NotNull AnnotatedElement element) {
        return null;
    }

    /**
     * Whether this provider wants to always parse quotes from the input.
     * <p>
     * If true, the input argument will have quotes parsed even if the command method or
     * parameter is not annotated with {@link Quoted}.
     *
     * @return true if quotes should always be parsed, false otherwise
     */
    default boolean alwaysParseQuotes() {
        return false;
    }
}
