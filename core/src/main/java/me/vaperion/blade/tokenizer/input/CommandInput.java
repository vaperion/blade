package me.vaperion.blade.tokenizer.input;

import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.tokenizer.StringTokenizer;
import me.vaperion.blade.tokenizer.TokenizerError;
import me.vaperion.blade.tokenizer.input.token.Token;
import me.vaperion.blade.tokenizer.input.token.impl.ArgumentToken;
import me.vaperion.blade.tokenizer.input.token.impl.FlagToken;
import me.vaperion.blade.tokenizer.input.token.impl.LabelToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.vaperion.blade.util.BladeHelper.arrayToEnumSet;

/**
 * Represents the tokenized form of a command input string.
 * <p>
 * This should be initialized with the whole input string, e.g.:
 * {@code /mycommand -b param1 -f flagValue param2}
 * <p>
 * While this class technically supports null commands, that should only be used in
 * special circumstances, such as when tokenizing for sub-command suggestions.
 */
@SuppressWarnings("unused")
public final class CommandInput {

    private final Blade blade;
    private final String input;
    private final EnumSet<InputOption> options;
    private final boolean endsInWhitespace;

    @Nullable
    private final BladeCommand command;

    private final StringTokenizer tokenizer;
    private final List<Token> tokens = new ArrayList<>();
    private boolean tokenized = false;

    @Nullable
    private String wholeLabel;

    public CommandInput(@NotNull Blade blade,
                        @Nullable BladeCommand command,
                        @NotNull String input) {
        this(blade, command, input, EnumSet.noneOf(InputOption.class));
    }

    public CommandInput(@NotNull Blade blade,
                        @Nullable BladeCommand command,
                        @NotNull String input,
                        @NotNull InputOption... options) {
        this(blade, command, input, arrayToEnumSet(InputOption.class, options));
    }

    public CommandInput(@NotNull Blade blade,
                        @Nullable BladeCommand command,
                        @NotNull String input,
                        @NotNull EnumSet<InputOption> options) {
        this.blade = blade;
        this.input = input.trim();
        this.options = options;
        this.endsInWhitespace = !input.isEmpty() &&
            Character.isWhitespace(input.charAt(input.length() - 1));

        this.command = command;
        this.tokenizer = new StringTokenizer(input.trim());
    }

    private void add(@NotNull Token token) {
        tokens.add(token);
    }

    /**
     * Get the original input string without a leading slash.
     *
     * @return the unslashed input string
     */
    @NotNull
    public String unslashedInput() {
        String input = input();

        if (input.startsWith("/"))
            return input.substring(1);

        return input;
    }

    /**
     * Get the original input string.
     *
     * @return the input string
     */
    @NotNull
    public String input() {
        return input;
    }

    /**
     * Get the associated BladeCommand.
     *
     * @return the BladeCommand
     */
    @Nullable
    public BladeCommand bladeCommand() {
        return command;
    }

    /**
     * Get the input options used during tokenization.
     *
     * @return an unmodifiable set of input options
     */
    @NotNull
    public EnumSet<InputOption> options() {
        return options;
    }

    /**
     * Get the list of tokens produced by tokenization.
     *
     * @return an unmodifiable list of tokens
     */
    @NotNull
    @Unmodifiable
    public List<Token> tokens() {
        return tokens;
    }

    /**
     * Check if the input string ended with whitespace.
     *
     * @return true if the input ended with whitespace, false otherwise
     */
    public boolean endsInWhitespace() {
        return endsInWhitespace;
    }

    /**
     * Get the label token, if present.
     */
    @NotNull
    public Optional<LabelToken> label() {
        return ofType(LabelToken.class)
            .findFirst();
    }

    /**
     * Get all argument tokens.
     */
    @NotNull
    public List<ArgumentToken> arguments() {
        return ofType(ArgumentToken.class)
            .collect(Collectors.toList());
    }

    /**
     * Get all flag tokens.
     */
    @NotNull
    public List<FlagToken> flags() {
        return ofType(FlagToken.class)
            .collect(Collectors.toList());
    }

    /**
     * Get a stream of tokens of the specified type.
     *
     * @param tokenClass the class of the token type to filter by
     * @param <T>        the type of the token
     * @return a stream of tokens of the specified type
     */
    @NotNull
    public <T extends Token> Stream<T> ofType(@NotNull Class<T> tokenClass) {
        if (!tokenized) {
            throw new IllegalStateException(
                "CommandInput must be tokenized before accessing its tokens."
            );
        }

        return tokens.stream()
            .filter(tokenClass::isInstance)
            .map(tokenClass::cast);
    }

    /**
     * Provide the already-resolved whole label (which may span multiple words)
     * so tokenization consumes it as the label in a single pass.
     * <p>
     * Without this, only the first word is treated as the label and the remaining label words
     * are emitted as argument tokens (to be merged afterwards via
     * {@link #mergeTokensToFormWholeLabel(String)}).
     * <p>
     * Must be called before {@link #tokenize()}.
     *
     * @param wholeLabel the resolved whole label
     */
    public void useWholeLabel(@NotNull String wholeLabel) {
        if (tokenized) {
            throw new IllegalStateException("Cannot set the whole label after tokenization.");
        }

        String trimmed = wholeLabel.trim();
        this.wholeLabel = trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Ensure that the input has been tokenized.
     *
     * @throws TokenizerError if a tokenization error occurs
     */
    public void ensureTokenized() throws TokenizerError {
        if (!tokenized) {
            blade.logger().warn("CommandInput was not tokenized before use. This is a bug in Blade, not your plugin. Please report it.");

            tokenize();
        }
    }

    /**
     * Tokenize the input command string.
     *
     * @throws TokenizerError if a tokenization error occurs
     */
    public void tokenize() throws TokenizerError {
        if (tokenized) return;
        tokenized = true;

        tokenizer.expect('/');
        tokenizer.skip();

        String label;
        if (wholeLabel != null) {
            String consumedLabel = consumeWholeLabel(wholeLabel);
            // Fall back if the input doesn't actually start with the expected whole label.
            label = consumedLabel != null ? consumedLabel : tokenizer.takeUnquotedString();
        } else {
            label = tokenizer.takeUnquotedString();
        }
        add(new LabelToken(label.toLowerCase(Locale.ROOT)));

        while (tokenizer.hasNext()) {
            tokenizer.expectWhitespace();
            tokenizer.skip();

            if (tokenizer.peek() == '-' && !options.contains(InputOption.DISALLOW_FLAGS)) {
                boolean allow =
                    !options.contains(InputOption.FLAGS_AT_END) ||
                        !tokenizer.remainingContainsWhitespace();

                if (allow) {
                    boolean parsed = potentiallyParseFlags();

                    if (!tokenizer.hasNext()) {
                        // We have to check again here, as the flag parsing may have consumed all input.
                        break;
                    }

                    if (parsed) {
                        // If we parsed flags, we have to skip this iteration,
                        // as the tokenizer position has already advanced.
                        continue;
                    }
                }
            }

            boolean parseQuoted = command != null && command.parseQuotes() ||
                options.contains(InputOption.ASSUME_QUOTED) ||
                nextParameterIsQuoted();

            String value = parseQuoted
                ? tokenizer.takeQuotedString(!options.contains(InputOption.STRICT_QUOTE_PARSING))
                : tokenizer.takeUnquotedString();

            add(new ArgumentToken(value));
        }
    }

    /**
     * Check if the next parameter to be parsed is defined as quoted.
     *
     * @return true if the next parameter is quoted, false otherwise
     */
    private boolean nextParameterIsQuoted() {
        if (command == null) {
            return false;
        }

        int argIndex = arguments().size();
        List<DefinedArgument> commandArguments = command.arguments();

        if (commandArguments.size() <= argIndex) {
            return false;
        }

        return commandArguments.get(argIndex).isQuoted();
    }

    /**
     * Consume the given whole label from the tokenizer, validating that the input actually
     * starts with it.
     *
     * @param wholeLabel the whole label to consume
     * @return the consumed label, or {@code null} if the input does not start with the expected label
     */
    @Nullable
    private String consumeWholeLabel(@NotNull String wholeLabel) {
        String[] words = wholeLabel.split("\\s+");
        StringBuilder consumed = new StringBuilder();

        tokenizer.saveCursor();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                if (!tokenizer.peekWhitespace()) {
                    // Missing separator (or end of input) before the next label word.
                    tokenizer.restoreCursor();
                    return null;
                }

                tokenizer.skipWhitespace();
                consumed.append(' ');
            }

            if (!tokenizer.hasNext()) {
                tokenizer.restoreCursor();
                return null;
            }

            String word = tokenizer.takeUnquotedString();
            if (!word.equalsIgnoreCase(words[i])) {
                tokenizer.restoreCursor();
                return null;
            }

            consumed.append(word);
        }

        tokenizer.dropSavedCursor();
        return consumed.toString();
    }

    /**
     * Potentially parse flags from the current tokenizer position.
     *
     * @return true if flags were parsed, false otherwise
     */
    private boolean potentiallyParseFlags() {
        tokenizer.saveCursor();
        tokenizer.skip();

        if (tokenizer.peek('-')) {
            // Long flag
            tokenizer.expect('-');
            tokenizer.skip();

            String name = tokenizer.takeUnquotedString();
            DefinedFlag flag = getFlagByName(name);

            if (flag == null) {
                // Unknown flag.

                if (!blade.configuration().lenientFlagMatching()) {
                    // Abort flag parsing.
                    tokenizer.restoreCursor();
                    return false;
                }

                // We're in lenient mode, just ignore the flag.
                tokenizer.dropSavedCursor();
                return true;
            }

            if (flag.isBooleanFlag()) {
                // Implicit flag

                add(FlagToken.builder()
                    .addImplicit(flag.getChar())
                    .build());

                tokenizer.dropSavedCursor();
                return true;
            }

            // Flag with value

            if (!tokenizer.hasNext()) {
                // We reached the end of the input but still need a value.
                throw TokenizerError.missingFlagValue(tokenizer, flag.getChar());
            }

            tokenizer.expectWhitespace();
            tokenizer.skip();

            String value = tokenizer.takeUnquotedString();
            add(FlagToken.builder()
                .add(flag.getChar(), value)
                .build());

            tokenizer.dropSavedCursor();
            return true;
        }

        // Short flag(s). Combined syntax is supported (e.g. -abc for -a -b -c).
        // Values will be read in order of flags. (e.g. -ab value1 value2 or -a value1 -b value2).
        List<Character> flagsNeedingValues = new ArrayList<>();
        FlagToken.Builder builder = FlagToken.builder();

        while (tokenizer.hasNext() && !tokenizer.peekWhitespace()) {
            char c = tokenizer.take();

            DefinedFlag flag = getFlagByChar(c);
            if (flag == null) {
                // Unknown flag.

                if (!blade.configuration().lenientFlagMatching()) {
                    // Abort flag parsing.
                    tokenizer.restoreCursor();
                    return false;
                }

                // We're in lenient mode, just ignore the flag.
                continue;
            }

            if (flag.isBooleanFlag()) {
                // Implicit flag
                builder.addImplicit(c);
            } else {
                // Flag with value
                flagsNeedingValues.add(c);
            }
        }

        while (!flagsNeedingValues.isEmpty()) {
            if (!tokenizer.hasNext()) {
                // We reached the end of the input but still have flags needing values.
                throw TokenizerError.missingFlagValue(tokenizer, flagsNeedingValues.get(0));
            }

            tokenizer.expectWhitespace();
            tokenizer.skip();

            String value = tokenizer.takeUnquotedString();
            char c = flagsNeedingValues.remove(0);
            builder.add(c, value);
        }

        add(builder.build());
        tokenizer.dropSavedCursor();
        return true;
    }

    /**
     * Merge tokens to form the given whole label.
     * <p>
     * This means that parameter tokens after the label will be deleted,
     * and merged according to the given whole label.
     * <p>
     * For example: <br/>
     * - Tokens: {@code [label: "my", arg: "command", arg: "param1"]} <br/>
     * - Whole label: {@code "my command"} <br/>
     * - Resulting tokens: {@code [label: "my command", arg: "param1"]}
     *
     * @param wholeLabel the whole label
     * @return true if the merge was successful, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean mergeTokensToFormWholeLabel(@NotNull String wholeLabel) {
        ensureTokenized();

        String[] parts = wholeLabel.split("\\s+");

        if (parts.length == 0)
            return false;

        LabelToken label = label().orElse(null);

        if (label == null) {
            return false;
        }

        if (label.name().equalsIgnoreCase(wholeLabel)) {
            // The whole label was already consumed during tokenization, nothing to merge.
            return true;
        }

        if (!parts[0].equalsIgnoreCase(label.name())) {
            // If the label doesn't match the input, we can't merge.
            return false;
        }

        List<ArgumentToken> arguments = arguments();

        if (arguments.size() < parts.length - 1) {
            // If there aren't enough arguments to match the whole label, we can't merge.
            return false;
        }

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i + 1];
            ArgumentToken argToken = arguments.get(i);

            if (!part.equalsIgnoreCase(argToken.value())) {
                // If any part doesn't match, we can't merge.
                return false;
            }
        }

        // Perform the merge
        label.name(wholeLabel);

        for (int i = 0; i < parts.length - 1; i++) {
            tokens.remove(arguments.get(i));
        }

        return true;
    }

    @Nullable
    private DefinedFlag getFlagByChar(char c) {
        if (command == null) return null;

        return command.flags().stream()
            .filter(flag -> flag.getChar() == c)
            .findFirst()
            .orElse(null);
    }

    @Nullable
    private DefinedFlag getFlagByName(@NotNull String name) {
        if (command == null) return null;

        return command.flags().stream()
            .filter(flag -> flag.getLongName().equals(name))
            .findFirst()
            .orElse(null);
    }

    @Override
    public String toString() {
        return "CommandInput{" +
            "input=`" + input + '`' +
            ", endsInWhitespace=" + endsInWhitespace +
            ", tokens=" + tokens +
            '}';
    }
}
