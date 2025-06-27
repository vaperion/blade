package me.vaperion.blade.exception;

/**
 * A special exception that causes the command usage message to be sent to the command executor
 * if thrown.
 */
public class BladeUsageMessage extends BladeExitMessage {
    public BladeUsageMessage() {
        super("");
    }
}