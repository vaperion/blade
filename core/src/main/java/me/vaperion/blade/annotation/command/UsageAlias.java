package me.vaperion.blade.annotation.command;

import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to set the alias that the usage message generation should use.
 * If this is not set, the first name specified in the {@link Command} annotation will be used.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UsageAlias {
    @Nullable
    String value();
}
