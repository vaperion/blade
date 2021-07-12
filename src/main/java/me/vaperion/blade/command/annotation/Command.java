package me.vaperion.blade.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Command {
    String[] value();

    boolean async() default false;

    boolean quoted() default true;

    String description() default "";

    /**
     * This data will get appended to the end of the usage message.
     */
    String extraUsageData() default "";
}
