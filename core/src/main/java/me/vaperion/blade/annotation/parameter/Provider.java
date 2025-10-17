package me.vaperion.blade.annotation.parameter;

import lombok.Getter;
import me.vaperion.blade.argument.ArgumentProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the argument provider for a parameter.
 * <p>
 * This allows you to customize argument parsing and/or tab completion for a specific parameter,
 * overriding the default provider registered for the parameter's type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Provider {
    /**
     * The argument provider class to use.
     *
     * @return the class of the argument provider
     */
    @NotNull
    Class<? extends ArgumentProvider<?>> value();

    /**
     * The scope of the override.
     *
     * @return the scope (defaults to {@link Scope#BOTH})
     */
    @NotNull
    Scope scope() default Scope.BOTH;

    /**
     * Defines which parts of the argument provider should be overridden.
     */
    @Getter
    enum Scope {
        /**
         * Override only the argument parser.
         */
        PARSER(true, false),

        /**
         * Override only the tab completion suggestions.
         */
        SUGGESTIONS(false, true),

        /**
         * Override both the parser and suggestions.
         */
        BOTH(true, true);

        private final boolean affectsParser;
        private final boolean affectsSuggestions;

        Scope(boolean affectsParser, boolean affectsSuggestions) {
            this.affectsParser = affectsParser;
            this.affectsSuggestions = affectsSuggestions;
        }
    }
}