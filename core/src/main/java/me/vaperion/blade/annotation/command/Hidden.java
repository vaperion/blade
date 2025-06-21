package me.vaperion.blade.annotation.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate that a command should be hidden.
 * Hidden commands do not show up in the generated help message, and cannot be tab completed.
 * Players will see the default unknown command message instead of the no permission message when executed without permission.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Hidden {
}
