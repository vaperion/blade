package me.vaperion.blade.util.command;

import org.jetbrains.annotations.NotNull;

/**
 * Quotes suggestions for quote-parsed arguments so that the completed input parses back to the suggested value.
 * <p>
 * Brigadier only accepts {@code 0-9}, {@code A-Z}, {@code a-z}, {@code _}, {@code -}, {@code .} and {@code +}
 * in an unquoted string argument, so any other character forces the suggestion into a quoted form.
 */
public final class SuggestionQuoting {

    private SuggestionQuoting() {
    }

    /**
     * Wraps the suggestion in double quotes if it contains any character that an unquoted argument cannot hold.
     *
     * @param suggestion the raw suggestion
     * @return the suggestion as it should be inserted into the command input
     */
    @NotNull
    public static String quoteIfNeeded(@NotNull String suggestion) {
        if (!suggestion.isEmpty() && suggestion.chars().allMatch(SuggestionQuoting::isAllowedUnquoted)) {
            return suggestion;
        }

        StringBuilder quoted = new StringBuilder(suggestion.length() + 2);
        quoted.append('"');

        for (int i = 0; i < suggestion.length(); i++) {
            char c = suggestion.charAt(i);

            if (c == '"' || c == '\\') {
                quoted.append('\\');
            }

            quoted.append(c);
        }

        quoted.append('"');
        return quoted.toString();
    }

    private static boolean isAllowedUnquoted(int c) {
        return (c >= '0' && c <= '9')
            || (c >= 'A' && c <= 'Z')
            || (c >= 'a' && c <= 'z')
            || c == '_' || c == '-' || c == '.' || c == '+';
    }
}
