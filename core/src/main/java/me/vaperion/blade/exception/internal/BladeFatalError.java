package me.vaperion.blade.exception.internal;

import me.vaperion.blade.exception.api.StacklessException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * An exception indicating that a command execution has failed.
 * <p>
 * The message will be shown to the command sender.
 */
@ApiStatus.Internal
@SuppressWarnings("unused")
public class BladeFatalError extends StacklessException {

    public BladeFatalError() {
        this("Command execution failed.");
    }

    public BladeFatalError(@NotNull String message) {
        super(message);
    }
}
