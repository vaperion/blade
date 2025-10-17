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

    @NotNull
    public static BladeParseError fatal(@NotNull String message) {
        return new BladeParseError(message);
    }

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
