package me.vaperion.blade.exception;

public class BladeExitMessage extends RuntimeException {

    public BladeExitMessage() {
        this("Command execution failed.");
    }

    public BladeExitMessage(String message) {
        super(message);
    }
}
