package me.vaperion.blade.annotation.command;

import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets a custom usage message for a command.
 * <p>
 * If not specified, a usage message will be generated automatically based on the command's parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Usage {
    @Nullable
    String value();
}
