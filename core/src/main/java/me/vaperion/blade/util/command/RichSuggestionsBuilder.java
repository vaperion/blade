package me.vaperion.blade.util.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A suggestions builder that supports rich completion metadata.
 * <p>
 * This is designed as an abstraction over Brigadier, while still
 * allowing legacy string-only suggestions.
 */
@SuppressWarnings("unused")
public interface RichSuggestionsBuilder {

    /**
     * Returns the full input used for suggestion generation.
     *
     * @return the full input string
     */
    @NotNull
    String input();

    /**
     * Returns the start offset where suggestions should replace text.
     *
     * @return the replacement start offset
     */
    int start();

    /**
     * Returns the remaining input segment from {@link #start()}.
     *
     * @return the remaining input segment
     */
    @NotNull
    String remaining();

    /**
     * Returns the lowercase remaining input segment from {@link #start()}.
     *
     * @return the lowercase remaining input segment
     */
    @NotNull
    String remainingLowerCase();

    /**
     * Adds a string suggestion.
     *
     * @param text the suggested text
     */
    void suggest(@NotNull String text);

    /**
     * Adds a string suggestion with optional tooltip metadata.
     *
     * @param text    the suggested text
     * @param tooltip the tooltip metadata, or null
     */
    default void suggest(@NotNull String text, @Nullable SuggestionTooltip tooltip) {
        suggest(text);
    }

    /**
     * Adds an integer suggestion.
     *
     * @param value the suggested value
     */
    void suggest(int value);

    /**
     * Adds an integer suggestion with optional tooltip metadata.
     *
     * @param value   the suggested value
     * @param tooltip the tooltip metadata, or null
     */
    default void suggest(int value, @Nullable SuggestionTooltip tooltip) {
        suggest(value);
    }

    /**
     * Adds all suggestions from another builder instance.
     *
     * @param other the builder to merge from
     */
    void add(@NotNull RichSuggestionsBuilder other);

    /**
     * Creates a new builder view with the given replacement offset.
     *
     * @param start the replacement start offset
     * @return a new offset builder
     */
    @NotNull
    RichSuggestionsBuilder createOffset(int start);

    /**
     * Creates a new builder view that restarts at this builder's start offset.
     *
     * @return a restarted builder
     */
    @NotNull
    RichSuggestionsBuilder restart();

    /**
     * Returns the legacy string-only builder view.
     *
     * @return the legacy suggestions builder
     */
    @NotNull
    SuggestionsBuilder legacyView();

    /**
     * Tests whether a suggestion passes the current filter.
     *
     * @param suggestion the suggestion to test
     * @return true if the suggestion passes the current filter, false otherwise
     */
    default boolean testFilter(@NotNull String suggestion) {
        return legacyView().testFilter(suggestion);
    }

    /**
     * Sets the suggestion filter.
     * <p>
     * Only suggestions that pass this filter will be included.
     *
     * @param filter the filter predicate, or null to accept all suggestions
     */
    default void setFilter(@Nullable Predicate<String> filter) {
        legacyView().setFilter(filter);
    }

    /**
     * Flushes legacy suggestions into this builder.
     */
    default void flushLegacy() {
    }
}
