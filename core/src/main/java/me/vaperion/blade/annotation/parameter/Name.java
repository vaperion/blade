package me.vaperion.blade.annotation.parameter;

import me.vaperion.blade.argument.ArgumentProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to set the name of a parameter.
 * <p>
 * Do note that this annotation is optional. If not present, the parameter name
 * included in the compiled bytecode will be used instead.
 * <p>
 * Usually these names are not very user-friendly (arg0, arg1, etc.),
 * but the compiler can be instructed to preserve parameter names using the
 * `-parameters` flag.
 * <p>
 * If the {@link ArgumentProvider} for the parameter provides a default name,
 * that name will be used instead of the bytecode name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Name {
    /**
     * The name to set for the parameter.
     *
     * @return the name of the parameter
     */
    @NotNull
    String value();
}
