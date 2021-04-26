package me.vaperion.blade.command.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WrappedSender<T> {

    @NotNull
    T getBackingSender();

    @NotNull
    String getName();

    void sendMessage(@NotNull String message);

    void sendMessage(@NotNull String... messages);

    @Nullable
    <T> T parseAs(@NotNull Class<T> clazz);

}
