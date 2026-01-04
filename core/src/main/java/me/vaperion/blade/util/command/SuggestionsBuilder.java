package me.vaperion.blade.util.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public final class SuggestionsBuilder {

    private final Set<String> suggestions = new HashSet<>();

    private Predicate<String> filter = s -> true;

    /**
     * Tests if a suggestion passes the current filter.
     *
     * @param suggestion the suggestion to test
     * @return true if the suggestion passes the filter, false otherwise
     */
    public boolean testFilter(@NotNull String suggestion) {
        return filter == null || filter.test(suggestion);
    }

    /**
     * Sets a filter for suggestions.
     * <p>
     * Only suggestions that pass the filter will be included in the final list.
     *
     * @param filter the filter predicate, or null to accept all suggestions
     */
    public void setFilter(@Nullable Predicate<String> filter) {
        if (filter != null) {
            this.filter = filter;
        } else {
            this.filter = s -> true;
        }
    }

    /**
     * Clears all current suggestions.
     */
    public void clear() {
        suggestions.clear();
    }

    /**
     * Adds a suggestion.
     * <p>
     * If the suggestion already exists, it will not be added again.
     *
     * @param suggestion the suggestion to add
     */
    public void suggest(@NotNull String suggestion) {
        if (!testFilter(suggestion)) {
            return;
        }

        suggestions.add(suggestion);
    }

    /**
     * Returns the number of suggestions currently stored.
     *
     * @return the count of suggestions
     */
    public int count() {
        return suggestions.size();
    }

    /**
     * Builds the list of suggestions.
     *
     * @return an unmodifiable list of suggestions
     */
    @NotNull
    @Unmodifiable
    public List<String> build() {
        return new ArrayList<>(suggestions);
    }
}
