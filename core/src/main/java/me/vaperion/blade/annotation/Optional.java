package me.vaperion.blade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate that a specific parameter is optional.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Optional {
    /**
     * The value will be used if the argument is not provided.
     * <p>
     * Special values:
     * <ul>
     * <li><code>null</code> => null</li>
     * <li><code>self</code> (for player types) => the sender</li>
     * </ul>
     * <p>
     * All other values will be parsed using the registered argument providers.
     */
    String value() default "null";

    /**
     * This method indicates whether the argument providers should return <code>null</code> instead of an exception if parsing fails.
     * <p>
     * <p>Example:
     * <p>Command declaration: <code>statsCommand(@Sender Player sender, @Name("player") @Optional Player target)</code>
     * <p>Executed commands:
     * <ul>
     * <li>/stats -> <code>null</code> gets passed because no argument was provided</li>
     * <li>/stats OnlinePlayer -> <code>OnlinePlayer</code> gets passed</li>
     * <li>/stats OfflinePlayer -> since the player is not online an exception would be thrown (printing an error to the user), but if ignoreFailedArgumentParse is true <code>null</code> gets returned instead</li>
     * </ul>
     */
    boolean ignoreFailedArgumentParse() default false;
}
