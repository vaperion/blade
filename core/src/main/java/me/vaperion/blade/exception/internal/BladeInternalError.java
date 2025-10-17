package me.vaperion.blade.exception.internal;

import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.exception.api.StacklessException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * An exception indicating that a command execution has failed.
 * <p>
 * The message will <b>NOT</b> be shown to the end user, unlike other
 * variants of this error like {@link BladeFatalError} and {@link BladeParseError}.
 */
@SuppressWarnings("unused")
@ApiStatus.Internal
public class BladeInternalError extends StacklessException {

    public BladeInternalError() {
        this("Command execution failed.");
    }

    public BladeInternalError(@NotNull String message) {
        super(message);
    }
}
