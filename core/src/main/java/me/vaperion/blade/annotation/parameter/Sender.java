package me.vaperion.blade.annotation.parameter;

import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.sender.SenderProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter as the command sender.
 * <p>
 * This parameter will be automatically populated with the user executing the command
 * and will not consume arguments from the command input.
 * <p>
 * You can register providers for custom types by implementing {@link SenderProvider} and
 * registering it via {@link Binder#bindSender(Class, SenderProvider)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Sender {
}
