package me.vaperion.blade.annotation.parameter;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to set the name of a parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Name {
    /**
     * The name to set for the parameter.
     *
     * @return the name of the parameter
     */
    @NotNull
    String value();
}
