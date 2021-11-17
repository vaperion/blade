package me.vaperion.blade.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WrappedSender<T> {

    @NotNull
    T getBackingSender();

    @NotNull
    String getName();

    void sendMessage(@NotNull String message);

    void sendMessage(@NotNull String... messages);

    @Nullable <S> S parseAs(@NotNull Class<S> clazz);

}
