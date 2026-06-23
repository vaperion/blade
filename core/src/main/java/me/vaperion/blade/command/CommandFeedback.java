package me.vaperion.blade.command;

import me.vaperion.blade.context.Context;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Represents formatted feedback for a command that can be sent to a context.
 */
public interface CommandFeedback {

    /**
     * Gets the formatted feedback text.
     *
     * @return the feedback component
     */
    @NotNull
    Component message();

    /**
     * Sends the feedback to the specified context.
     *
     * @param context the context to send the feedback to
     */
    default void sendTo(@NotNull Context context) {
        context.sender().sendMessage(message());
    }
}
