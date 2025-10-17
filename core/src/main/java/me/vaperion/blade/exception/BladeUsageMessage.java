package me.vaperion.blade.exception;

import me.vaperion.blade.exception.api.StacklessException;

/**
 * A special exception that causes the command usage message to be sent to the command executor
 * if thrown.
 */
@SuppressWarnings("unused")
public class BladeUsageMessage extends StacklessException {
    public BladeUsageMessage() {
        super("");
    }
}