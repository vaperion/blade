package me.vaperion.blade.annotation.argument;

import me.vaperion.blade.argument.ArgumentProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to override the tab completer for the specific argument.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Completer {
    @NotNull
    Class<? extends ArgumentProvider<?>> value();
}