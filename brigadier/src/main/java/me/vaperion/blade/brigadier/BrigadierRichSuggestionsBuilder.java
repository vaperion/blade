package me.vaperion.blade.brigadier;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.vaperion.blade.util.command.RichSuggestionsBuilder;
import me.vaperion.blade.util.command.SuggestionTooltip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class BrigadierRichSuggestionsBuilder implements RichSuggestionsBuilder {

    private final SuggestionsBuilder builder;
    private final me.vaperion.blade.util.command.SuggestionsBuilder legacy;
    private Predicate<String> filter;

    public BrigadierRichSuggestionsBuilder(@NotNull SuggestionsBuilder builder) {
        this(builder, s -> true);
    }

    private BrigadierRichSuggestionsBuilder(
        @NotNull SuggestionsBuilder builder,
        @NotNull Predicate<String> filter) {
        this.builder = builder;
        this.legacy = new me.vaperion.blade.util.command.SuggestionsBuilder();
        this.filter = filter;
        this.legacy.setFilter(filter);
    }

    @Override
    public @NotNull String input() {
        return builder.getInput();
    }

    @Override
    public int start() {
        return builder.getStart();
    }

    @Override
    public @NotNull String remaining() {
        return builder.getRemaining();
    }

    @Override
    public @NotNull String remainingLowerCase() {
        return builder.getRemainingLowerCase();
    }

    @Override
    public void suggest(@NotNull String text) {
        if (text.equals(builder.getRemaining()) || !testFilter(text)) {
            return;
        }

        builder.suggest(text);
    }

    @Override
    public void suggest(@NotNull String text, @Nullable SuggestionTooltip tooltip) {
        if (text.equals(builder.getRemaining()) || !testFilter(text)) {
            return;
        }

        if (tooltip == null) {
            builder.suggest(text);
            return;
        }

        builder.suggest(text, mapTooltip(tooltip));
    }

    @Override
    public void suggest(int value) {
        if (!testFilter(String.valueOf(value))) {
            return;
        }

        builder.suggest(value);
    }

    @Override
    public void suggest(int value, @Nullable SuggestionTooltip tooltip) {
        if (!testFilter(String.valueOf(value))) {
            return;
        }

        if (tooltip == null) {
            builder.suggest(value);
            return;
        }

        builder.suggest(value, mapTooltip(tooltip));
    }

    @Override
    public void add(@NotNull RichSuggestionsBuilder other) {
        if (!(other instanceof BrigadierRichSuggestionsBuilder richBuilder)) {
            throw new IllegalArgumentException("Cannot add suggestions from a different builder type");
        }

        builder.add(richBuilder.builder);
    }

    @Override
    public @NotNull RichSuggestionsBuilder createOffset(int start) {
        return new BrigadierRichSuggestionsBuilder(builder.createOffset(start), filter);
    }

    @Override
    public @NotNull RichSuggestionsBuilder restart() {
        return createOffset(builder.getStart());
    }

    @Override
    public @NotNull me.vaperion.blade.util.command.SuggestionsBuilder legacyView() {
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
        for (String suggestion : legacy.build()) {
            if (!suggestion.equals(builder.getRemaining())) {
                builder.suggest(suggestion);
            }
        }

        legacy.clear();
    }

    private @NotNull Message mapTooltip(@Nullable SuggestionTooltip tooltip) {
        if (tooltip == null) {
            return new LiteralMessage("");
        }

        Message message = tooltip.as(Message.class);
        if (message != null) {
            return message;
        }

        String text = tooltip.text();

        if (text == null) {
            return new LiteralMessage("");
        }

        return new LiteralMessage(text);
    }
}
