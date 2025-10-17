package me.vaperion.blade.exception.internal;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * An exception indicating that command invocation has failed.
 */
@SuppressWarnings("unused")
@ApiStatus.Internal
public class BladeInvocationError extends RuntimeException {

    public BladeInvocationError(@NotNull String message,
                                @NotNull Throwable cause) {
        super(message, cause);
    }
}
