package me.vaperion.blade.annotation;

import me.vaperion.blade.argument.BladeProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Completer {
    Class<? extends BladeProvider<?>> provider();
}
