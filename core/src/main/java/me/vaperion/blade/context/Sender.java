package me.vaperion.blade.context;

import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Sender<T> {
    /**
     * Get the raw platform instance of this wrapped sender.
     * <p>
     * This is the actual platform-specific sender type, such as a Player, CommandSource, etc.
     * </p>
     *
     * @return the platform sender instance
     */
    @NotNull
    T rawSender();

    /**
     * Get the underlying platform-specific sender object.
     * <p>
     * On most platforms, this will be the same as {@code getSender()},
     * but some platforms may use proxies or dynamic types. (e.g. Fabric's CommandSource)
     *
     * @return the underlying platform-specific sender object
     */
    @ApiStatus.Internal
    @NotNull
    Object underlyingSender();

    /**
     * Get the class type of the underlying sender.
     * <p>
     * On most platforms, this will be the same as {@code getSender().getClass()},
     * but some platforms may use proxies or dynamic types. (e.g. Fabric's CommandSource)
     *
     * @return the class type of the underlying sender
     */
    @ApiStatus.Internal
    @NotNull
    Class<?> underlyingSenderType();

    /**
     * Get the name of this sender.
     *
     * @return the name of the sender
     */
    @NotNull
    String name();

    /**
     * Check if this sender has a specific permission.
     *
     * @param permission the permission to check
     * @return true if the sender has the permission, false otherwise
     */
    boolean hasPermission(@NotNull String permission);

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

    /**
     * Check if this wrapped sender is of the expected type for the given command.
     *
     * @param command the command to check against
     * @return true if the sender is of the expected type, false otherwise
     */
    @ApiStatus.Internal
    boolean isExpectedType(@NotNull BladeCommand command);
}
