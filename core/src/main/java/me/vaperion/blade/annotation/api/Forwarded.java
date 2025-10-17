package me.vaperion.blade.annotation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an annotation as forwarded to argument providers.
 * <p>
 * Forwarded annotations are ignored by the parameter resolver and instead passed directly
 * to the argument provider's suggest/provide methods.
 * <p>
 * This allows you to create custom annotations that carry additional type-safe information
 * for argument providers without interfering with parameter resolution.
 * <p>
 * This is a type-safe alternative to {@link me.vaperion.blade.annotation.parameter.Data @Data},
 * allowing you to define structured metadata instead of using strings.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Forwarded {
}
