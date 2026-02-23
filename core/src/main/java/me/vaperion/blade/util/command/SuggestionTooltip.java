package me.vaperion.blade.util.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A tooltip container for rich command suggestions.
 */
@SuppressWarnings("unused")
public interface SuggestionTooltip {

    /**
     * Returns the tooltip text representation.
     *
     * @return the tooltip text, or null if not provided
     */
    @Nullable
    default String text() {
        return null;
    }

    /**
     * Returns a typed native tooltip handle.
     *
     * @param type the requested native type
     * @param <T>  the requested native type
     * @return the native handle, or null if unavailable
     */
    @Nullable
    default <T> T as(@NotNull Class<T> type) {
        return null;
    }

    /**
     * Creates a plain text tooltip.
     *
     * @param text the tooltip text
     * @return a tooltip backed by plain text
     */
    @NotNull
    static SuggestionTooltip text(@NotNull String text) {
        return new BasicSuggestionTooltip(text, null);
    }

    /**
     * Creates a tooltip backed by a native handle.
     *
     * @param handle the native tooltip handle
     * @return a tooltip backed by the native handle
     */
    @NotNull
    static SuggestionTooltip handle(@NotNull Object handle) {
        return new BasicSuggestionTooltip(String.valueOf(handle), handle);
    }

    /**
     * Creates a tooltip backed by a native handle with explicit text fallback.
     *
     * @param handle the native tooltip handle
     * @param text   the fallback text representation, or null
     * @return a tooltip backed by the native handle
     */
    @NotNull
    static SuggestionTooltip of(@NotNull Object handle, @Nullable String text) {
        return new BasicSuggestionTooltip(text, handle);
    }

    /**
     * Basic tooltip implementation used by factory methods.
     */
    final class BasicSuggestionTooltip implements SuggestionTooltip {
        private final String text;
        private final Object handle;

        private BasicSuggestionTooltip(@Nullable String text,
                                       @Nullable Object handle) {
            this.text = text;
            this.handle = handle;
        }

        @Override
        public @Nullable String text() {
            return text;
        }

        @Override
        public <T> @Nullable T as(@NotNull Class<T> type) {
            if (!type.isInstance(handle)) {
                return null;
            }

            return type.cast(handle);
        }
    }
}
