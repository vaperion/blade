package me.vaperion.blade.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Optional {
    /**
     * The provided value will be used if the argument is not provided.
     * <p>
     * Reserved values:
     * <ul>
     * <li><code>null</code> => passes null</li>
     * <li><code>self</code> (for player types) => passes the sender</li>
     * </ul>
     * <p>
     * Any other value will be parsed.
     */
    String value() default "null";
}
