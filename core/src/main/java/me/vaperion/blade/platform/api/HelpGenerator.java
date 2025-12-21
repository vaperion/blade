package me.vaperion.blade.platform.api;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.internal.BladeFatalError;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

@FunctionalInterface
public interface HelpGenerator<Text> {
    int RESULTS_PER_PAGE = 8;

    /**
     * Generates help text for the given list of commands within the provided context.
     *
     * @param context  the context in which the help is generated
     * @param commands the list of commands to generate help for
     * @return a list of help text entries
     */
    @NotNull
    List<Text> generate(@NotNull Context context, @NotNull List<BladeCommand> commands);

    /**
     * Filters the provided list of commands based on visibility and a filter input.
     *
     * @param context     the context in which the filtering is performed
     * @param commands    the list of commands to filter
     * @param filterInput the input string used for filtering commands
     * @return a list of filtered commands
     */
    @ApiStatus.NonExtendable
    @NotNull
    default List<BladeCommand> filterCommands(@NotNull Context context,
                                              @NotNull List<BladeCommand> commands,
                                              @NotNull String filterInput) {
        List<BladeCommand> visibleCommands = commands.stream()
            .distinct()
            .filter(c -> !c.hidden() && !c.helpCommand())
            .sorted(context.blade().configuration().helpSorter())
            .collect(Collectors.toList());

        List<BladeCommand> filtered = visibleCommands.stream()
            .filter(c -> c.anyLabelStartsWith(filterInput))
            .collect(Collectors.toList());

        // if filter has no results but there are visible commands, show them instead
        return filtered.isEmpty() && !visibleCommands.isEmpty()
            ? visibleCommands
            : filtered;
    }

    @SuppressWarnings("unused")
    final class Default<T> implements HelpGenerator<T> {
        @NotNull
        @Override
        public List<T> generate(@NotNull Context context, @NotNull List<BladeCommand> commands) {
            throw new BladeFatalError("This command failed to execute as we couldn't find its registration.");
        }
    }
}
