package me.vaperion.blade.annotation.command;

import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to set the main label of the command, which will be shown
 * in the usage message, generated help, and other places.
 * <p>
 * If this is not set, the first name specified in the {@link Command} annotation will be used.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MainLabel {
    @Nullable
    String value();
}
