package me.vaperion.blade.platform.api;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.CommandFeedback;
import me.vaperion.blade.exception.internal.BladeFatalError;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CommandFeedbackCreator<Text> {

    /**
     * Creates the feedback message for the given command.
     *
     * @param command the Blade command
     * @param isUsage whether this is usage feedback (adds "Usage: " prefix)
     * @return the feedback message
     */
    @NotNull
    CommandFeedback<Text> create(@NotNull BladeCommand command, boolean isUsage);

    @SuppressWarnings("unused")
    final class Default<T> implements CommandFeedbackCreator<T> {
        @Override
        public @NotNull CommandFeedback<T> create(@NotNull BladeCommand command, boolean isUsage) {
            throw new BladeFatalError("No CommandFeedbackCreator registered for platform");
        }
    }

}
