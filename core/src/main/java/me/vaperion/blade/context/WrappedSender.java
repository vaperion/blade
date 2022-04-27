package me.vaperion.blade.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WrappedSender<T> {
    @NotNull T getSender();

    @NotNull String getName();

    boolean hasPermission(@NotNull String permission);

    void sendMessage(@NotNull String message);

    void sendMessage(@NotNull String... messages);

    @Nullable <S> S parseAs(@NotNull Class<S> clazz);
}
