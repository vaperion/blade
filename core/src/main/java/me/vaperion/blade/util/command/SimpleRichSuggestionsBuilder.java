package me.vaperion.blade.util.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public final class SimpleRichSuggestionsBuilder implements RichSuggestionsBuilder {

    private final String input;
    private final int start;
    private final String remaining;
    private final String remainingLowerCase;

    private final SuggestionsBuilder legacy;
    private final Set<String> suggestions = new LinkedHashSet<>();
    private Predicate<String> filter;

    public SimpleRichSuggestionsBuilder(@NotNull String input, int start) {
        this(input, start, s -> true);
    }

    private SimpleRichSuggestionsBuilder(@NotNull String input,
                                         int start,
                                         @NotNull Predicate<String> filter) {
        this.input = input;
        this.start = start;
        this.remaining = input.substring(start);
        this.remainingLowerCase = input.toLowerCase(Locale.ROOT).substring(start);
        this.legacy = new SuggestionsBuilder();
        this.filter = filter;
        this.legacy.setFilter(filter);
    }

    @Override
    public @NotNull String input() {
        return input;
    }

    @Override
    public int start() {
        return start;
    }

    @Override
    public @NotNull String remaining() {
        return remaining;
    }

    @Override
    public @NotNull String remainingLowerCase() {
        return remainingLowerCase;
    }

    @Override
    public void suggest(@NotNull String text) {
        if (text.equals(remaining) || !legacy.testFilter(text)) {
            return;
        }

        suggestions.add(text);
    }

    @Override
    public void suggest(int value) {
        String text = String.valueOf(value);

        if (!legacy.testFilter(text)) {
            return;
        }

        suggestions.add(text);
    }

    @Override
    public void add(@NotNull RichSuggestionsBuilder other) {
        if (!(other instanceof SimpleRichSuggestionsBuilder)) {
            throw new IllegalArgumentException("Cannot add suggestions from a different builder type");
        }

        SimpleRichSuggestionsBuilder builder = (SimpleRichSuggestionsBuilder) other;
        suggestions.addAll(builder.suggestions);
    }

    @Override
    public @NotNull RichSuggestionsBuilder createOffset(int start) {
        return new SimpleRichSuggestionsBuilder(input, start, filter);
    }

    @Override
    public @NotNull RichSuggestionsBuilder restart() {
        return createOffset(start);
    }

    @Override
    public @NotNull SuggestionsBuilder legacyView() {
        return legacy;
    }

    @Override
    public boolean testFilter(@NotNull String suggestion) {
        return filter == null || filter.test(suggestion);
    }

    @Override
    public void setFilter(Predicate<String> filter) {
        if (filter != null) {
            this.filter = filter;
        } else {
            this.filter = s -> true;
        }

        legacy.setFilter(this.filter);
    }

    @Override
    public void flushLegacy() {
        suggestions.addAll(legacy.build());
        legacy.clear();
    }

    @NotNull
    @Unmodifiable
    public List<String> build() {
        return new ArrayList<>(suggestions);
    }
}
