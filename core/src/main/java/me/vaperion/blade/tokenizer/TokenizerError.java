package me.vaperion.blade.tokenizer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@SuppressWarnings("unused")
public class TokenizerError extends RuntimeException {

    @NotNull
    public static TokenizerError unexpectedEnd(@NotNull AbstractStringTokenizer tokenizer) {
        return new TokenizerError(
            Type.UNEXPECTED_END,
            tokenizer,
            0,
            "unexpected end of input"
        );
    }

    @NotNull
    public static TokenizerError unexpectedCharacter(@NotNull AbstractStringTokenizer tokenizer,
                                                     @Nullable Character expected,
                                                     char actual) {
        return new TokenizerError(
            Type.UNEXPECTED_CHARACTER,
            tokenizer,
            0,
            String.format(
                "expected character %s but found '%c'",
                expected == null
                    ? "that matches a predicate"
                    : "'" + expected + "'",
                actual
            )
        );
    }

    @NotNull
    public static TokenizerError requiredCharacters(@NotNull AbstractStringTokenizer tokenizer,
                                                    int count,
                                                    int actual) {
        return new TokenizerError(
            Type.REQUIRED_N_CHARACTERS,
            tokenizer,
            0,
            String.format(
                "expected at least %d more %s, but only %d %s left",
                count,
                count == 1 ? "character" : "characters",
                actual,
                actual == 1 ? "is" : "are"
            ));
    }

    @NotNull
    public static TokenizerError missingFlagValue(@NotNull AbstractStringTokenizer tokenizer,
                                                  char flag) {
        return new TokenizerError(
            Type.MISSING_FLAG_VALUE,
            tokenizer,
            0,
            String.format(
                "flag '-%c' requires a value but none was provided",
                flag
            ));
    }

    private final Type type;
    private final AbstractStringTokenizer tokenizer;
    private final int cursorOffset;

    TokenizerError(@NotNull Type type,
                   @NotNull AbstractStringTokenizer tokenizer,
                   int cursorOffset,
                   @NotNull String error) {
        super(error);

        this.type = type;
        this.tokenizer = tokenizer;
        this.cursorOffset = cursorOffset;
        setStackTrace(new StackTraceElement[0]);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    @NotNull
    public String formatForChat() {
        return String.format(
            "Syntax error at position %d: %s",
            tokenizer.cursorPosition() + cursorOffset,
            getMessage()
        );
    }

    @Getter
    public enum Type {
        UNEXPECTED_END,
        UNEXPECTED_CHARACTER,
        REQUIRED_N_CHARACTERS,
        MISSING_FLAG_VALUE(true),
        ;

        private final boolean isSilent;

        Type() {
            this(false);
        }

        Type(boolean isSilent) {
            this.isSilent = isSilent;
        }
    }

    private static final String BLUE = "\u001B[34m";
    private static final String RED = "\u001B[31m";
    private static final String WHITE = "\u001B[37m";
    private static final String BOLD = "\u001B[1m";
    private static final String RESET = "\u001B[0m";

    @NotNull
    public static String generateFancyMessage(@NotNull TokenizerError error) {
        StringBuilder builder = new StringBuilder();
        int pos = error.tokenizer.cursorPosition() + error.cursorOffset;

        builder.append("\n");
        builder.append(BLUE).append("  --> ").append(RESET);
        builder.append(WHITE).append("input:").append(pos + 1).append(RESET).append("\n");
        builder.append(BLUE).append("   |\n").append(RESET);
        builder.append(BLUE).append(" 1 | ").append(RESET);
        builder.append(WHITE).append(error.tokenizer.string()).append(RESET).append("\n");
        builder.append(BLUE).append("   | ").append(RESET);

        for (int i = 0; i < pos; i++) {
            builder.append(" ");
        }
        builder.append(RED).append(BOLD).append("^").append(RESET);
        builder.append(" ").append(RED).append(BOLD).append(error.getMessage()).append(RESET);
        builder.append("\n");

        return builder.toString();
    }
}
