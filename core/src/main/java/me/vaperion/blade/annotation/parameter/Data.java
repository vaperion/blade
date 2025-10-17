package me.vaperion.blade.annotation.parameter;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Passes custom string data to an argument provider's suggest/provide methods.
 * <p>
 * This allows you to customize argument provider behavior on a per-parameter basis
 * without creating separate provider classes.
 * <p>
 * For type-safe metadata, consider creating a custom annotation marked with
 * {@link me.vaperion.blade.annotation.api.Forwarded @Forwarded} instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Data {
    /**
     * The data to pass to the argument provider.
     *
     * @return the data to pass to the argument provider
     */
    @NotNull
    String[] value();
}