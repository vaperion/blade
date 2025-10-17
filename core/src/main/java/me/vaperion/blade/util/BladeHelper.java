package me.vaperion.blade.util;

import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.command.Command;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.context.Sender;
import me.vaperion.blade.sender.internal.SndProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@SuppressWarnings("unused")
public interface BladeHelper {

    String ERROR_MESSAGE = "An error occurred while executing this command. " +
        "Contact a server administrator if this continues.";

    @ApiStatus.Internal
    @Nullable
    static Object parseSender(@NotNull Blade blade,
                              @NotNull List<SndProvider<?>> providers,
                              @NotNull Context context,
                              @NotNull Sender<?> sender) {
        for (SndProvider<?> provider : providers) {
            Object result = provider.provider().provide(context, sender);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    @ApiStatus.Internal
    @NotNull
    static List<String> generateLabels(@NotNull Command command,
                                       @Nullable Command parentCommand) {
        if (parentCommand == null)
            return Arrays.asList(command.value());

        List<String> labels = new ArrayList<>();

        for (String label : command.value()) {
            for (String parentLabel : parentCommand.value()) {
                String fullLabel = parentLabel + " " + label;
                labels.add(fullLabel.trim().toLowerCase());
            }
        }

        return labels;
    }

    @NotNull
    static String join(@NotNull CharSequence separator,
                       @NotNull Iterable<?> elements) {
        return join(separator, elements, 0, Integer.MAX_VALUE);
    }

    @NotNull
    static String join(@NotNull CharSequence separator,
                       @NotNull Iterable<?> elements,
                       int start,
                       int end) {
        StringBuilder sb = new StringBuilder();

        int index = 0;
        for (Object element : elements) {
            if (index >= start && index < end) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
                sb.append(element);
            }

            index++;
        }

        return sb.toString();
    }

    @NotNull
    static <T extends Enum<T>> EnumSet<T> arrayToEnumSet(@NotNull Class<T> type,
                                                         @NotNull T[] array) {
        if (array.length == 0) {
            return EnumSet.noneOf(type);
        }

        return EnumSet.copyOf(Arrays.asList(array));
    }

    @NotNull
    static String mergeLabelWithArgs(@NotNull String label,
                                     @NotNull String[] args) {
        if (args.length == 0) {
            return label;
        }

        return label + " " + String.join(" ", args);
    }

    @NotNull
    static String mergeLabelWithArgs(@NotNull String label,
                                     @NotNull String args) {
        return args.isEmpty()
            ? label
            : label + " " + args;
    }

    @NotNull
    static String removePrefix(@NotNull String input,
                               @NotNull String prefix) {
        if (input.startsWith(prefix)) {
            return input.substring(prefix.length());
        }

        return input;
    }

    @NotNull
    static String removeCommandQualifier(@NotNull String input) {
        boolean startsWithSlash = input.startsWith("/");
        int firstSpace = input.indexOf(' ');
        int colonIndex = input.indexOf(':');

        if (colonIndex == -1) {
            return input;
        }

        if (firstSpace != -1 && colonIndex > firstSpace) {
            return input;
        }

        if (firstSpace == -1) {
            return (startsWithSlash ? "/" : "") +
                input.substring(colonIndex + 1);
        }

        return (startsWithSlash ? "/" : "") +
            input.substring(colonIndex + 1) +
            input.substring(firstSpace);
    }

}
