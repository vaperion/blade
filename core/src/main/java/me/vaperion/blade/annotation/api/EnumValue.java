package me.vaperion.blade.annotation.api;

import me.vaperion.blade.argument.impl.EnumArgument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures enum parsing behavior for the default enum argument provider ({@link EnumArgument}).
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EnumValue {
    /**
     * Primary display name for this enum value.
     * <p>
     * Defaults to the enum constant name when blank.
     *
     * @return the preferred display name
     */
    String name() default "";

    /**
     * Additional accepted names for this enum value.
     *
     * @return accepted aliases
     */
    String[] aliases() default {};

    /**
     * Priority used to resolve ambiguous matches.
     * <p>
     * Higher values win when multiple enum values match the input.
     *
     * @return priority value
     */
    int priority() default 0;

    /**
     * Whether this enum value should be hidden from suggestions.
     *
     * @return true if hidden from suggestions
     */
    boolean hidden() default false;

    /**
     * Whether this enum value should be blocked from usage.
     *
     * @return true if blocked from parsing
     */
    boolean block() default false;
}
