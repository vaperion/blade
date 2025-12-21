package me.vaperion.blade.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class ErrorMessage {

    @NotNull
    public static ErrorMessage showCommandUsage() {
        return new ErrorMessage(Type.SHOW_COMMAND_USAGE, Collections.emptyList());
    }

    @NotNull
    public static ErrorMessage showCommandHelp() {
        return new ErrorMessage(Type.SHOW_COMMAND_HELP, Collections.emptyList());
    }

    @NotNull
    public static ErrorMessage lines(@NotNull List<String> lines) {
        return new ErrorMessage(Type.LINES, lines);
    }

    @NotNull
    public static ErrorMessage lines(@NotNull String... lines) {
        return new ErrorMessage(Type.LINES, Arrays.asList(lines));
    }

    private final Type type;
    private final List<String> lines;

    ErrorMessage(@NotNull Type type,
                 @NotNull List<String> lines) {
        this.type = type;
        this.lines = lines;
    }

    @NotNull
    public Type type() {
        return type;
    }

    @NotNull
    public List<String> lines() {
        return lines;
    }

    public enum Type {
        LINES,
        SHOW_COMMAND_USAGE,
        SHOW_COMMAND_HELP
    }
}
