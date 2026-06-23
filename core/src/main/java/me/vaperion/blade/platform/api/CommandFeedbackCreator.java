package me.vaperion.blade.platform.api;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.CommandFeedback;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CommandFeedbackCreator {

    /**
     * Creates the feedback message for the given command.
     *
     * @param command the Blade command
     * @param isUsage whether this is usage feedback (adds "Usage: " prefix)
     * @return the feedback message
     */
    @NotNull
    CommandFeedback create(@NotNull BladeCommand command, boolean isUsage);

}
