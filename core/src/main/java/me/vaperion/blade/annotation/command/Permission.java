package me.vaperion.blade.annotation.command;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to set the permission of the command.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Permission {
    /**
     * The required permission.
     * <p>
     * If the permission is not set, the command will be executed without any permission check.
     * <p>
     * If you prefix the permission with {@code "@"}, it will be treated as a permission predicate,
     * and checked accordingly against the registered permission predicates. If the predicate is not registered,
     * the command will be executed without any permission check.
     * <p>
     * Special values:
     * <ul>
     *   <li>{@code "op"} - Requires the sender to be an operator</li>
     *   <li>{@code "console"} - Requires the sender to be the console</li>
     * </ul>
     */
    @NotNull
    String value() default "";

    /**
     * The message that gets displayed if the player does not have permission to execute the command.
     * <p>
     * If the message is not set, the default message will be displayed, set in the {@link me.vaperion.blade.Blade} builder.
     */
    @NotNull
    String message() default "";
}