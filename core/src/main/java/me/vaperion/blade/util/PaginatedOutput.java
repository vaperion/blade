package me.vaperion.blade.util;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public abstract class PaginatedOutput<T> {

    private final int resultsPerPage;

    public abstract String formatErrorMessage(Error error, Object... args);

    public abstract String getHeader(int page, int totalPages);

    public abstract String getFooter(int page, int totalPages);

    public abstract String formatLine(T result, int index);

    public final List<String> generatePage(List<T> results, int page) {
        if (results.isEmpty()) {
            return Collections.singletonList(formatErrorMessage(Error.NO_RESULTS));
        }

        int totalPages = results.size() / resultsPerPage + (results.size() % resultsPerPage == 0 ? 0 : 1);
        if (page < 1 || page > totalPages) {
            return Collections.singletonList(formatErrorMessage(Error.PAGE_OUT_OF_BOUNDS, page, totalPages));
        }

        int startIndex = (page - 1) * resultsPerPage;
        int endIndex = Math.min(startIndex + resultsPerPage, results.size());

        List<String> lines = new ArrayList<>();

        String header = getHeader(page, totalPages);
        if (header != null) lines.addAll(Arrays.asList(header.split("\n", -1)));

        for (T result : results.subList(startIndex, endIndex)) {
            String line = formatLine(result, startIndex + lines.size());
            if (line == null) continue;
            lines.addAll(Arrays.asList(line.split("\n", -1)));
        }

        String footer = getFooter(page, totalPages);
        if (footer != null) lines.addAll(Arrays.asList(footer.split("\n", -1)));

        return lines;
    }

    public enum Error {
        NO_RESULTS,
        PAGE_OUT_OF_BOUNDS
    }

}