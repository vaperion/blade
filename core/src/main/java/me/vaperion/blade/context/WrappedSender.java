package me.vaperion.blade.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WrappedSender<T> {
    /**
     * Get the underlying sender of this wrapped sender.
     * <p>
     * This is the actual platform-specific sender type, such as a Player, CommandSender, etc.
     * </p>
     *
     * @return the underlying sender
     */
    @NotNull
    T getSender();

    /**
     * Get the name of this sender.
     *
     * @return the name of the sender
     */
    @NotNull
    String getName();

    /**
     * Check if this sender has a specific permission.
     *
     * @param permission the permission to check
     * @return true if the sender has the permission, false otherwise
     */
    boolean hasPermission(@NotNull String permission);

    /**
     * Send a message to this sender.
     *
     * @param message the message to send
     */
    void sendMessage(@NotNull String message);

    /**
     * Send multiple messages to this sender.
     *
     * @param messages the messages to send
     */
    void sendMessage(@NotNull String... messages);

    /**
     * Attempt to "parse" (or convert) the underlying platform-specific sender
     * type to the given class type.
     * <p>
     * If the conversion is not possible, this method will return null.
     * </p>
     *
     * @param clazz the class to parse the sender as
     * @return the parsed sender if successful, null otherwise
     */
    @Nullable
    <S> S parseAs(@NotNull Class<S> clazz);
}
