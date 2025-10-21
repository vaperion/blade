package me.vaperion.blade.exception;

import lombok.Getter;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.exception.api.StacklessException;
import me.vaperion.blade.sender.SenderProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when an {@link ArgumentProvider} or {@link SenderProvider}
 * fails to parse a command argument.
 * <p>
 * The message will be shown to the command sender.
 */
@Getter
@SuppressWarnings("unused")
public class BladeParseError extends StacklessException {

    /**
     * Creates a fatal parse error.
     * <p>
     * This will cause command execution to fail. The message will be shown to the command sender.
     *
     * @param message the error message
     * @return the parse error
     */
    @NotNull
    public static BladeParseError fatal(@NotNull String message) {
        return new BladeParseError(message);
    }

    /**
     * Creates a recoverable parse error.
     * <p>
     * This allows command execution to complete if the parameter is optional.
     * In that case, the parameter will be set to null and the error will be silently ignored.
     *
     * @param message the error message
     * @return the parse error
     */
    @NotNull
    public static BladeParseError recoverable(@NotNull String message) {
        return new BladeParseError(message, true);
    }

    private final boolean isRecoverable;

    BladeParseError(@NotNull String message, boolean recoverable) {
        super(message);
        this.isRecoverable = recoverable;
    }

    BladeParseError(@NotNull String message) {
        this(message, false);
    }
}
