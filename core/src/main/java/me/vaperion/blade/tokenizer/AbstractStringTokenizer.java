package me.vaperion.blade.tokenizer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A simple class to help tokenize strings.
 */
@SuppressWarnings({ "unused" })
public abstract class AbstractStringTokenizer {

    public static final char ESCAPE = '\\';
    public static final char BACKTICK = '`';
    public static final char SINGLE_QUOTE = '\'';
    public static final char DOUBLE_QUOTE = '\"';

    public static final Predicate<Character> ESCAPE_PRED = c -> c == ESCAPE;
    public static final Predicate<Character> WHITESPACE_PRED = Character::isWhitespace;
    public static final Predicate<Character> SINGLE_QUOTE_PRED = c -> c == SINGLE_QUOTE;
    public static final Predicate<Character> DOUBLE_QUOTE_PRED = c -> c == DOUBLE_QUOTE;
    public static final Predicate<Character> QUOTE_PRED = SINGLE_QUOTE_PRED.or(DOUBLE_QUOTE_PRED);

    private String string;
    private int cursor, savedCursor;

    public AbstractStringTokenizer(String string) {
        this.string = string;
    }

    /**
     * Returns the string to tokenize.
     *
     * @return the string to tokenize
     */
    @NotNull
    public String string() {
        return string;
    }

    /**
     * Sets the string to tokenize.
     *
     * @param string the string to tokenize
     */
    public void setString(@NotNull String string) {
        this.string = string;
    }

    /**
     * Consumes a single character at the current cursor position.
     *
     * @return the character consumed
     */
    public char take() throws TokenizerError {
        if (!hasNext()) {
            throw TokenizerError.unexpectedEnd(this);
        }

        return string().charAt(cursor++);
    }

    /**
     * Expects the current character to be whitespace.
     */
    public void expectWhitespace() throws TokenizerError {
        expect(WHITESPACE_PRED);
    }

    /**
     * Expects the current character to be the given character.
     *
     * @param expected the expected character
     */
    public void expect(char expected) throws TokenizerError {
        if (peek() == expected) {
            return;
        }

        throw TokenizerError.unexpectedCharacter(this, expected, peek());
    }

    /**
     * Expects the current character to pass the given test.
     *
     * @param predicate the predicate to test the character
     */
    public void expect(@NotNull Predicate<Character> predicate) throws TokenizerError {
        if (predicate.test(peek())) {
            return;
        }

        throw TokenizerError.unexpectedCharacter(this, null, peek());
    }

    /**
     * Returns whether the tokenizer has remaining characters.
     *
     * @return true if there are remaining characters
     */
    public boolean hasNext() {
        return hasNext(0);
    }

    /**
     * Returns whether the tokenizer has remaining characters at the given offset.
     *
     * @param offset the offset from the current cursor position
     * @return true if there are remaining characters at the offset
     */
    public boolean hasNext(int offset) {
        return cursor + offset < length();
    }

    /**
     * Ensures that at least the specified number of characters remain.
     *
     * @param count the minimum number of characters required
     */
    public void require(int count) throws TokenizerError {
        if (remainingLength() < count) {
            throw TokenizerError.requiredCharacters(this, count, remainingLength());
        }
    }

    /**
     * Returns the character at the current cursor position without consuming it.
     *
     * @return the character at the current position
     */
    public char peek() throws TokenizerError {
        return peek(0);
    }

    /**
     * Returns the character at the given offset from the current cursor position without consuming it.
     *
     * @param offset the offset from the current cursor position (can be negative)
     * @return the character at the offset
     */
    public char peek(int offset) throws TokenizerError {
        if (!hasNext(offset)) {
            throw TokenizerError.requiredCharacters(this, 1, 0);
        }

        return string().charAt(cursor + offset);
    }

    /**
     * Checks if the current character is a whitespace character.
     */
    public boolean peekWhitespace() throws TokenizerError {
        if (!hasNext()) {
            return false;
        }

        return WHITESPACE_PRED.test(peek());
    }

    /**
     * Checks if the current character matches the given character.
     *
     * @param c the character to match
     */
    public boolean peek(char c) throws TokenizerError {
        if (!hasNext()) {
            return false;
        }

        return peek() == c;
    }

    /**
     * Checks if the current character matches the given predicate.
     *
     * @param predicate the predicate to test the character
     */
    public boolean peek(@NotNull Predicate<Character> predicate) throws TokenizerError {
        if (!hasNext()) {
            return false;
        }

        return predicate.test(peek());
    }

    /**
     * Checks if the remaining string contains whitespace anywhere.
     *
     * @return true if the remaining string contains whitespace
     */
    public boolean remainingContainsWhitespace() {
        for (int i = cursor; i < length(); i++) {
            if (WHITESPACE_PRED.test(string().charAt(i))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Skips one character.
     */
    public void skip() throws TokenizerError {
        skip(1);
    }

    /**
     * Skips the specified number of characters.
     *
     * @param offset the amount to skip (can be negative)
     */
    public void skip(int offset) throws TokenizerError {
        if (offset > 0 && cursor + offset > length()) {
            throw TokenizerError.requiredCharacters(this, offset, remainingLength());
        }

        cursor += offset;
    }

    /**
     * Consumes a specified number of characters.
     *
     * @param offset the number of characters to consume
     * @return the consumed string
     */
    @NotNull
    public String take(int offset) throws TokenizerError {
        if (offset < 0 || cursor + offset > length()) {
            throw TokenizerError.requiredCharacters(this, offset, remainingLength());
        }

        int start = cursor;
        cursor += offset;
        return string().substring(start, cursor);
    }

    /**
     * Resets the cursor position to the beginning.
     */
    public void resetCursor() {
        cursor = 0;
    }

    /**
     * Sets the cursor position to the specified index.
     *
     * @param index the new cursor position
     */
    public void setCursor(int index) {
        cursor = index;
    }

    /**
     * Saves the current cursor position.
     */
    public void saveCursor() {
        savedCursor = cursor;
    }

    /**
     * Restores the cursor to the last saved position.
     */
    public void restoreCursor() {
        if (savedCursor == -1) {
            throw new IllegalStateException("No cursor position has been saved");
        }

        cursor = savedCursor;
        savedCursor = -1;
    }

    /**
     * Drops the saved cursor position.
     */
    public void dropSavedCursor() {
        savedCursor = -1;
    }

    /**
     * Returns the consumed characters as a string.
     *
     * @return the consumed characters as a string
     */
    @NotNull
    public String consumed() {
        return string().substring(0, cursor);
    }

    /**
     * Returns the remaining characters as a string.
     *
     * @return the remaining characters as a string
     */
    @NotNull
    public String remaining() {
        return string().substring(cursor);
    }

    /**
     * Returns the current cursor position.
     *
     * @return the cursor position
     */
    public int cursorPosition() {
        return cursor;
    }

    /**
     * Returns the number of remaining characters.
     *
     * @return the number of remaining characters
     */
    public int remainingLength() {
        return length() - cursor;
    }

    /**
     * Returns the original length of the string.
     *
     * @return the original length of the string
     */
    public int length() {
        return string().length();
    }

    /**
     * Skips all consecutive occurrences of the given character.
     *
     * @param c the character to skip
     */
    public void skipWhile(char c) {
        skipWhile(ch -> ch == c);
    }

    /**
     * Skips all consecutive characters matching the given predicate.
     *
     * @param c the predicate to test the characters
     */
    public void skipWhile(Predicate<Character> c) {
        while (hasNext() && c.test(peek())) {
            skip();
        }
    }

    /**
     * Skips all characters until the given character is found.
     *
     * @param c the character to stop at
     */
    public void skipUntil(char c) {
        skipUntil(ch -> ch == c);
    }

    /**
     * Skips all characters until the given predicate is matched.
     *
     * @param predicate the predicate to test the characters
     */
    public void skipUntil(Predicate<Character> predicate) {
        while (hasNext() && !predicate.test(peek())) {
            skip();
        }
    }

    /**
     * Skips all consecutive whitespace characters.
     */
    public void skipWhitespace() {
        while (hasNext() && WHITESPACE_PRED.test(peek())) {
            skip();
        }
    }

    /**
     * Consumes a quoted (single or double) or unquoted string.
     *
     * @return the consumed string, or null if there's nothing left or if the quoted string is not terminated
     */
    @Nullable
    public String takeQuotedString() {
        return takeQuotedString(true);
    }

    /**
     * Consumes a quoted (single or double) or unquoted string.
     *
     * @param lenient whether to allow unterminated quoted strings
     * @return the consumed string, or null if there's nothing left or if the quoted string is not terminated (and `lenient` is false)
     */
    @Nullable
    public String takeQuotedString(boolean lenient) {
        return takeString(QUOTE_PRED, lenient, lenient);
    }

    /**
     * Consumes a quoted or unquoted string based on the given quote predicate.
     *
     * @param predicate the predicate to test for quote characters
     * @return the consumed string, or null if there's nothing left or if the quoted string is not terminated
     */
    @Nullable
    public String takeString(@NotNull Predicate<Character> predicate) {
        return takeString(predicate, false, false);
    }

    /**
     * Consumes a quoted or unquoted string based on the given quote predicate.
     *
     * @param predicate         the predicate to test for quote characters
     * @param allowEmpty        whether to allow empty strings
     * @param allowUnterminated whether to allow unterminated quoted strings
     * @return the consumed string, or null if there's nothing left (and `allowEmpty` is false) or if the quoted string is not terminated (and `allowUnterminated` is false)
     */
    @Nullable
    public String takeString(@NotNull Predicate<Character> predicate,
                             boolean allowEmpty,
                             boolean allowUnterminated) {
        if (!hasNext())
            return allowEmpty ? "" : null;

        char c = peek();

        if (predicate.test(c)) {
            skip();
            return takeUntil(allowUnterminated, c);
        } else {
            return takeUnquotedString();
        }
    }

    /**
     * Consumes an unquoted string (until whitespace or end of input).
     *
     * @return the consumed unquoted string
     */
    @NotNull
    public String takeUnquotedString() {
        int start = cursor;
        skipUntil(WHITESPACE_PRED);
        return string().substring(start, cursor);
    }

    /**
     * Consumes all characters until the terminator character is found.
     * Supports escaping the terminator with a backslash.
     *
     * @param terminator the terminator character
     * @return the consumed string, or null if the sequence is not properly terminated
     */
    @Nullable
    public String takeUntil(char terminator) {
        return takeUntil(false, terminator);
    }

    /**
     * Consumes all characters until the terminator character is found.
     * Supports escaping the terminator with a backslash.
     *
     * @param allowUnterminated whether to allow unterminated sequences
     * @param terminator        the terminator character
     * @return the consumed string, or null if the sequence is not properly terminated (and `allowUnterminated` is false)
     */
    @Nullable
    public String takeUntil(boolean allowUnterminated, char terminator) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        while (hasNext()) {
            char c = take();

            if (escaped) {
                if (c == terminator || c == ESCAPE) {
                    result.append(c);
                    escaped = false;
                } else {
                    cursor--;
                    return null;
                }
                continue;
            }

            if (c == ESCAPE) {
                escaped = true;
            } else if (c == terminator) {
                return result.toString();
            } else {
                result.append(c);
            }
        }

        return allowUnterminated ? result.toString() : null;
    }
}
