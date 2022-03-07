package me.vaperion.blade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Command {
    /**
     * The names you can use to execute this command.
     * <p>Example: <code>{"a", "b"}</code> => <code>/a</code>; <code>/b</code>
     */
    String[] value();

    /**
     * This method indicates whether this command should be executed asynchronously or not.
     * <p>This is useful for interacting with a database in a command, so you don't have to do the threading manually.
     */
    boolean async() default false;

    /**
     * This method indicates whether quotes (<code>'</code>) and double quotes (<code>"</code>) should be parsed or not.
     * <p>If this is true, <code>"hello world"</code> will turn into one string argument instead of two (<code>"hello</code> and <code>world"</code>).
     */
    boolean quoted() default false;

    /**
     * This method indicates whether this command should be hidden from or not.
     * <p>Hidden commands do not show up in the generated help message, and cannot be tab completed.</p>
     * <p>Players will see the default unknown command message instead of the no permission message when executed without permission.</p>
     */
    boolean hidden() default false;

    /**
     * This is the description of the command that is shown when you hover over the usage message.
     */
    String description() default "";

    /**
     * This is the usage message that is shown when an invalid number of arguments is given.
     * <p>If this is not set, the usage message will automatically be generated.</p>
     */
    String usage() default "";

    /**
     * This is the alias that should be displayed in the generated help message.
     * <p>If this is not set, the first alias in {@link Command#value()} will be used.</p>
     */
    String usageAlias() default "";

    /**
     * This data will get appended to the end of the generated usage message.
     * <p>If a custom usage message is set, this will be ignored.</p>
     */
    String extraUsageData() default "";
}
