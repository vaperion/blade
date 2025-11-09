package me.vaperion.blade.impl.node;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.Container;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.vaperion.blade.util.BladeHelper.join;
import static me.vaperion.blade.util.BladeHelper.removeCommandQualifier;

/**
 * Resolves command input strings to their corresponding command nodes.
 *
 * @see ResolvedCommandNode
 * @see BladeCommand
 */
@SuppressWarnings("unused")
@RequiredArgsConstructor
public final class CommandNodeResolver {

    private final Blade blade;

    /**
     * Resolves the command node for the given input.
     *
     * @param input the input command string
     * @return the resolved command node, or {@code null} if no matching commands are found
     */
    @Nullable
    public ResolvedCommandNode resolve(@NotNull String input) {
        input = removeCommandQualifier(input.toLowerCase(Locale.ROOT).trim());

        if (input.startsWith("/"))
            input = input.substring(1);

        ResolvedCommandNode exact = findExactMatch(input);

        if (exact != null) {
            return exact;
        }

        String label = input.split(" ")[0];

        List<BladeCommand> commands = blade.labelToCommands()
            .getOrDefault(label, Collections.emptyList());

        if (commands.isEmpty())
            return null;

        List<ResolvedCommandNode> nodes = new ArrayList<>();

        for (BladeCommand command : commands) {
            nodes.add(ResolvedCommandNode.match(command.labels()[0], command));
        }

        return ResolvedCommandNode.stub(nodes);
    }

    /**
     * Attempts to find an exact match for the given input.
     *
     * @param input the normalized input string
     * @return a match node if an exact command is found, or {@code null} if no match exists
     */
    @ApiStatus.Internal
    @Nullable
    public ResolvedCommandNode findExactMatch(@NotNull String input) {
        String[] parts = input.split(" ");

        if (parts.length == 0)
            return null;

        List<BladeCommand> commands = blade.labelToCommands()
            .getOrDefault(parts[0], Collections.emptyList());

        // Iterate through all possible commands with this label

        do {
            for (BladeCommand cmd : commands) {
                for (String label : cmd.labels()) {
                    if (label.equalsIgnoreCase(input))
                        return ResolvedCommandNode.match(label, cmd);
                }
            }

            // cut off the last word
            parts = input.split(" ");
            input = join(" ", Arrays.asList(parts),
                0, parts.length - 1);
        } while (!input.isEmpty());

        return null;
    }

    /**
     * Finds the container associated with the given resolved command node.
     *
     * @param node the resolved command node
     * @param <T>  the type of container to find
     * @return the found container, or {@code null} if none is found
     */
    @Nullable
    public <T extends Container> T findContainer(@NotNull ResolvedCommandNode node) {
        if (node.command() != null) {
            T container = findContainer(node.command());

            if (container != null)
                return container;
        }

        for (ResolvedCommandNode subcommand : node.subcommands()) {
            T container = findContainer(subcommand);

            if (container != null)
                return container;
        }

        return null;
    }

    /**
     * Finds the container associated with the given command.
     *
     * @param command the command to find the container for
     * @param <T>     the type of container to find
     * @return the found container, or {@code null} if none is found
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Container> T findContainer(@NotNull BladeCommand command) {
        for (String label : command.labels()) {
            String realLabel = label.split(" ")[0];

            Container container = blade.labelToContainer().get(realLabel);

            if (container != null) {
                return (T) container;
            }
        }

        return null;
    }

}
