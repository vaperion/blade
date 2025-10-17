package me.vaperion.blade.annotation.parameter;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to create a flag.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Flag {
    /**
     * The character value of the flag that can be used as -f.
     *
     * @return the flag character
     */
    char value();

    /**
     * The long flag name that can be used as --longName.
     *
     * @return the long flag name
     */
    @NotNull
    String longName() default "";

    /**
     * The description of the flag.
     *
     * @return the flag description
     */
    @NotNull
    String description() default "";

    /**
     * Whether the flag is required or not.
     *
     * @return true if the flag is required, false otherwise
     */
    boolean required() default false;
}