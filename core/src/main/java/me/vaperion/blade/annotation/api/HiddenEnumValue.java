package me.vaperion.blade.annotation.api;

import me.vaperion.blade.argument.impl.EnumArgument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an enum value should be hidden from suggestions, optionally blocking its usage as well.
 * <p>
 * This only applies to enum parameters if the default provider is used ({@link EnumArgument}).
 *
 * @deprecated use {@link EnumValue} for more flexible control over enum values
 */
@SuppressWarnings("unused")
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HiddenEnumValue {
    /**
     * Whether to block the usage of the annotated enum value.
     *
     * @return true if the enum value should be blocked, false otherwise
     */
    boolean block() default true;
}
