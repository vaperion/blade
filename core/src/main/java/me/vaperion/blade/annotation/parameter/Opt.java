package me.vaperion.blade.annotation.parameter;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate that a specific parameter is optional.
 * <p>
 * This used to be called {@code Optional}, but was renamed to avoid confusion with
 * {@link java.util.Optional}.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Opt {

    /**
     * The type of default value to use when the argument is not provided.
     *
     * @return the type of the optional value
     *
     * @see Type
     */
    @NotNull
    Type value() default Type.EMPTY_OR_CUSTOM;

    /**
     * The custom value that should be used if {@link #value()} is set to {@link Type#CUSTOM}.
     *
     * @return the custom value as a string
     */
    @NotNull
    String custom() default "";

    /**
     * Whether a parsing error should result in a {@link Type#EMPTY} value instead of failing command execution.
     * <p>
     * When true, unparseable arguments are treated as empty and errors are silently ignored.
     *
     * @return true if parsing errors should be treated as empty, false otherwise
     */
    boolean treatErrorAsEmpty() default false;

    /**
     * The type of the optional value.
     */
    @SuppressWarnings("unused")
    enum Type {
        /**
         * Use either {@link #EMPTY} or {@link #CUSTOM} based on whether a custom value is provided.
         */
        EMPTY_OR_CUSTOM,
        /**
         * Use {@code null} as the default value.
         * <p>
         * For primitive types, this will use the default value of the primitive
         * (e.g., 0 for int, false for boolean).
         */
        EMPTY,
        /**
         * Use the command sender as the default value (for player types).
         * <p>
         * For non-player types, this will result in an error at command execution.
         */
        SENDER,
        /**
         * Use the custom value specified by {@link Opt#custom()}.
         * <p>
         * This will be parsed according to the parameter type.
         */
        CUSTOM,
    }
}
