package me.vaperion.blade.util.command;

import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Decides whether a command should be registered.
 */
@FunctionalInterface
public interface CommandRegistrationPredicate extends Predicate<BladeCommand> {

    /**
     * Tests whether the given command should be registered.
     *
     * @param command the command being registered
     * @return {@code true} to register the command, {@code false} to skip it
     */
    @Override
    boolean test(@NotNull BladeCommand command);
}
