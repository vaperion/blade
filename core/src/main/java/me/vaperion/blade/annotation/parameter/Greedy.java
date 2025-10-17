package me.vaperion.blade.annotation.parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that the annotated {@linkplain String} parameter should be
 * parsed as greedy, meaning that it will contain the rest of the arguments combined into a
 * single string.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Greedy {
}
