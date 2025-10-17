package me.vaperion.blade.annotation.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables quote parsing for command arguments.
 * <p>
 * When present, arguments wrapped in quotes are treated as a single argument.
 * <p>
 * For example, {@code /test "hello world"} will be parsed as a single argument {@code hello world}
 * instead of two separate arguments {@code "hello} and {@code world"}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Quoted {
}
