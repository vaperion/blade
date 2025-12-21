package me.vaperion.blade.command;

import me.vaperion.blade.context.Context;
import org.jetbrains.annotations.NotNull;

/**
 * Represents formatted feedback for a command that can be sent to a context.
 * <p>
 * Implementations are responsible for constructing formatted text components
 * and delivering them to command senders through the context.
 *
 * @param <Text> the type of text component used for the feedback
 */
public interface CommandFeedback<Text> {
    /**
     * Gets the formatted feedback text.
     *
     * @return the feedback text component
     */
    @NotNull
    Text message();

    /**
     * Sends the feedback to the specified context.
     *
     * @param context the context to send the feedback to
     */
    void sendTo(@NotNull Context context);
}
