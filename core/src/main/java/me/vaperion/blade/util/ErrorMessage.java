package me.vaperion.blade.util;

import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public static ErrorMessage showCommandUsage(@Nullable BladeCommand command) {
        return new ErrorMessage(Type.SHOW_COMMAND_USAGE, Collections.emptyList(), command);
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
    private final BladeCommand command;

    ErrorMessage(@NotNull Type type,
                 @NotNull List<String> lines) {
        this(type, lines, null);
    }

    ErrorMessage(@NotNull Type type,
                 @NotNull List<String> lines,
                 @Nullable BladeCommand command) {
        this.type = type;
        this.lines = lines;
        this.command = command;
    }

    @NotNull
    public Type type() {
        return type;
    }

    @NotNull
    public List<String> lines() {
        return lines;
    }

    @Nullable
    public BladeCommand command() {
        return command;
    }

    public enum Type {
        LINES,
        SHOW_COMMAND_USAGE,
        SHOW_COMMAND_HELP
    }
}
