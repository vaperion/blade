package me.vaperion.blade.platform.api;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.CommandFeedback;
import me.vaperion.blade.context.Context;
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

    /**
     * Creates the feedback message for the given command, localized for the
     * given context's sender through the configured {@link CommandLocalizer}.
     *
     * @param command the Blade command
     * @param isUsage whether this is usage feedback (adds "Usage: " prefix)
     * @param context the context whose sender the feedback is localized for
     * @return the feedback message
     */
    @NotNull
    default CommandFeedback create(@NotNull BladeCommand command,
                                   boolean isUsage,
                                   @NotNull Context context) {
        return create(command, isUsage);
    }

}
