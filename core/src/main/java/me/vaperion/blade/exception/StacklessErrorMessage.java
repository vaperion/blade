package me.vaperion.blade.exception;

import org.jetbrains.annotations.NotNull;

public class StacklessErrorMessage extends RuntimeException {

    public StacklessErrorMessage(@NotNull String message, Object... args) {
        super(String.format(message, args));
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
