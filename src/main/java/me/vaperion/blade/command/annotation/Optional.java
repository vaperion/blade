package me.vaperion.blade.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Optional {
    /**
     * The value will be used if the argument is not provided.
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

    /**
     * This method indicates whether <code>null</code> is allowed if an argument is provided.
     * <p>
     * <p>Example:
     * <p>Command declaration: <code>statsCommand(@Sender Player sender, @Name("player") @Optional Player target)</code>
     * <p>Executed commands:
     * <ul>
     * <li>/stats -> <code>null</code> gets passed because no argument was provided</li>
     * <li>/stats RealPlayer -> <code>RealPlayer</code> gets passed</li>
     * <li>/stats FakePlayer -> If {@link #ignoreFailedArgumentParse()} returns true, <code>null</code> will be passed, otherwise usage will be shown</li>
     * </ul>
     */
    boolean ignoreFailedArgumentParse() default false;
}
