package me.vaperion.blade.annotation.parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the allowed range for numeric parameters.
 * <p>
 * Both bounds are inclusive. Use {@link Double#NaN} (default) to leave a bound unspecified.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Range {
    /**
     * The minimum allowed value (inclusive).
     * <p>
     * Defaults to {@link Double#NaN} (no minimum).
     *
     * @return the minimum value
     */
    double min() default Double.NaN;

    /**
     * The maximum allowed value (inclusive).
     * <p>
     * Defaults to {@link Double#NaN} (no maximum).
     *
     * @return the maximum value
     */
    double max() default Double.NaN;
}