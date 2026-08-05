package me.vaperion.blade.annotation.command;

import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the description for a command or parameter.
 * <p>
 * Command descriptions are displayed in help messages, while parameter
 * descriptions are shown as a hover in the usage message.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface Description {
    /**
     * The description.
     *
     * @return the description string
     */
    @Nullable
    String value();
}
