package me.vaperion.blade.annotation.argument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark an annotation as a forwarded annotation.
 * <p>
 * These annotations are ignored by the parameter resolver,
 * instead passing the annotation to the suggest/provide methods.
 * <p>
 * This lets you create optional annotations that can be used
 * to carry additional type-safe information for argument providers.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ForwardedAnnotation {
}
