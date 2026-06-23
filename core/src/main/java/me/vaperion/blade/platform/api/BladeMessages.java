package me.vaperion.blade.platform.api;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface BladeMessages {

    /**
     * The message shown when no command matches the input.
     *
     * @return the unknown command message
     */
    @NotNull
    Component unknownCommand();

    /**
     * The message shown when the sender lacks permission for a command.
     *
     * @param command the command the sender attempted to run
     * @return the permission message
     */
    @NotNull
    Component permissionMessage(@NotNull BladeCommand command);

    /**
     * The message shown when the sender lacks permission to view any help entries.
     *
     * @param context the active command context
     * @return the no-permission message
     */
    @NotNull
    Component noHelpPermission(@NotNull Context context);

    /**
     * Wraps an arbitrary error string into a component.
     *
     * @param message the raw message
     * @return the error component
     */
    @NotNull
    Component error(@NotNull String message);

    /**
     * The generic message shown when an unexpected error occurs while executing a command.
     *
     * @return the generic error message
     */
    @NotNull
    Component genericError();
}
