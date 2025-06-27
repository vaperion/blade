package me.vaperion.blade.exception;

import org.jetbrains.annotations.NotNull;

/**
 * A special exception whose message is sent to the command executor
 * if thrown.
 * <p>
 * This should only be used in {@link me.vaperion.blade.argument.ArgumentProvider ArgumentProvider}s and {@link me.vaperion.blade.sender.SenderProvider SenderProvider}s.
 * Any other usage is discouraged and unsupported.
 * </p>
 */
public class BladeExitMessage extends RuntimeException {

    public BladeExitMessage() {
        this("Command execution failed.");
    }

    public BladeExitMessage(@NotNull String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // Don't fill stack trace
        return this;
    }
}
