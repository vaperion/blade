package me.vaperion.blade.util.command;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public abstract class PaginatedOutput<Value, Text> {

    private final int resultsPerPage;

    /**
     * Formats an error message based on the provided error type and arguments.
     *
     * @param error the type of error
     * @param args  additional arguments for formatting the error message
     * @return the formatted error message
     */
    @NotNull
    public abstract Text error(@NotNull Error error, Object... args);

    /**
     * Generates the header text for a specific page.
     *
     * @param page       the current page number
     * @param totalPages the total number of pages
     * @return the header text for the specified page
     */
    @Nullable
    public abstract Text header(int page, int totalPages);

    /**
     * Generates the footer text for a specific page.
     *
     * @param page       the current page number
     * @param totalPages the total number of pages
     * @return the footer text for the specified page
     */
    @Nullable
    public abstract Text footer(int page, int totalPages);

    /**
     * Formats a single line of output for a given result and its index.
     *
     * @param result the result to format
     * @param index  the index of the result
     * @return the formatted line of output
     */
    @Nullable
    public abstract Text line(Value result, int index);

    @NotNull
    public final List<Text> generatePage(@NotNull List<Value> results, int page) {
        if (results.isEmpty()) {
            return Collections.singletonList(
                error(Error.NO_RESULTS));
        }

        int totalPages = results.size() / resultsPerPage + (results.size() % resultsPerPage == 0 ? 0 : 1);
        if (page < 1 || page > totalPages) {
            return Collections.singletonList(
                error(Error.PAGE_OUT_OF_BOUNDS, page, totalPages));
        }

        int startIndex = (page - 1) * resultsPerPage;
        int endIndex = Math.min(startIndex + resultsPerPage, results.size());

        List<Text> lines = new ArrayList<>();

        Text header = header(page, totalPages);
        if (header != null) lines.add(header);

        for (Value result : results.subList(startIndex, endIndex)) {
            Text line = line(result, startIndex + lines.size());
            if (line == null) continue;
            lines.add(line);
        }

        Text footer = footer(page, totalPages);
        if (footer != null) lines.add(footer);

        return lines;
    }

    public enum Error {
        NO_RESULTS,
        PAGE_OUT_OF_BOUNDS
    }

}