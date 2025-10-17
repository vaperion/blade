package me.vaperion.blade.exception.api;

import org.jetbrains.annotations.NotNull;

public class StacklessException extends RuntimeException {

    public StacklessException(@NotNull String message, Object... args) {
        super(String.format(message, args));
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
