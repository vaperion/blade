package me.vaperion.blade.impl.node;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.vaperion.blade.util.BladeHelper.join;
import static me.vaperion.blade.util.BladeHelper.removeCommandQualifier;

@RequiredArgsConstructor
public final class CommandNodeResolver {

    private final Blade blade;

    /**
     * Resolves the command node for the given input.
     *
     * @param input the input command string
     * @return the resolved command node, or null if not found
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
            ResolvedCommandNode subMatch = resolve(command.labels()[0]);

            if (subMatch == null)
                nodes.add(ResolvedCommandNode.root(command.labels()[0], command));
            else
                nodes.add(subMatch);
        }

        return ResolvedCommandNode.stub(nodes);
    }

    @ApiStatus.Internal
    @Nullable
    public ResolvedCommandNode findExactMatch(@NotNull String input) {
        String[] parts = input.split(" ");

        if (parts.length == 0)
            return null;

        List<BladeCommand> commands = blade.labelToCommands()
            .getOrDefault(parts[0], Collections.emptyList());

        if (commands.size() <= 1) {
            // Only one command with this label or no command at all

            if (commands.isEmpty())
                return null;

            BladeCommand candidate = commands.get(0);

            for (String label : candidate.labels()) {
                if (label.equalsIgnoreCase(input)) {
                    return ResolvedCommandNode.root(label, candidate);
                }
            }

            return null;
        }

        // Iterate through all possible commands with this label

        do {
            for (BladeCommand cmd : commands) {
                for (String label : cmd.labels()) {
                    if (label.equalsIgnoreCase(input))
                        return ResolvedCommandNode.root(label, cmd);
                }
            }

            // cut off the last word
            parts = input.split(" ");
            input = join(" ", Arrays.asList(parts),
                0, parts.length - 1);
        } while (!input.isEmpty());

        return null;
    }

}
