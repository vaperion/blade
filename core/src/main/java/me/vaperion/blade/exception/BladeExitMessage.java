package me.vaperion.blade.exception;

import org.jetbrains.annotations.NotNull;

public class BladeExitMessage extends RuntimeException {

    public BladeExitMessage() {
        this("Command execution failed.");
    }

    public BladeExitMessage(@NotNull String message) {
        super(message);
    }
}
