package me.vaperion.blade.impl.node;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.Container;
import me.vaperion.blade.tree.CommandTree;
import me.vaperion.blade.tree.CommandTreeNode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static me.vaperion.blade.util.BladeHelper.join;
import static me.vaperion.blade.util.BladeHelper.removeCommandQualifier;

/**
 * Resolves command input strings to their corresponding command nodes.
 *
 * @see BladeCommand
 */
@SuppressWarnings("unused")
@RequiredArgsConstructor
public final class CommandResolver {

    private final Blade blade;

    /**
     * Resolves the command node for the given input.
     * <p>
     * For exact matches, returns the specific command.
     * For partial matches, returns a stub containing all possible subcommands.
     *
     * @param input the input command string
     * @return the resolved command node, or {@code null} if no matching commands are found
     */
    @Nullable
    public ResolvedCommand resolve(@NotNull String input) {
        input = removeCommandQualifier(input.toLowerCase(Locale.ROOT).trim());

        if (input.startsWith("/"))
            input = input.substring(1);

        ResolvedCommand exact = findExactMatch(input);
        if (exact != null) {
            return exact;
        }

        String label = input.split(" ")[0];
        CommandTree tree = blade.commandTree();
        CommandTreeNode rootNode = tree.root(label);

        if (rootNode == null)
            return null;

        CommandTreeNode current = rootNode;
        String[] parts = input.split(" ");

        for (int i = 1; i < parts.length; i++) {
            CommandTreeNode child = current.child(parts[i]);
            if (child == null) break;
            current = child;
        }

        List<ResolvedCommand> nodes = new ArrayList<>();

        if (current.isLeaf()) {
            nodes.add(ResolvedCommand.match(current.label(), current.command()));
        }

        for (CommandTreeNode child : current.children().values()) {
            addNodeRecursively(child, nodes);
        }

        if (nodes.isEmpty())
            return null;

        return ResolvedCommand.stub(nodes);
    }

    /**
     * Attempts to find an exact command match.
     *
     * @param input the normalized input string
     * @return a match node if an exact command is found, or {@code null} if no match exists
     */
    @ApiStatus.Internal
    @Nullable
    public ResolvedCommand findExactMatch(@NotNull String input) {
        String[] parts = input.split(" ");

        if (parts.length == 0)
            return null;

        CommandTree tree = blade.commandTree();
        CommandTreeNode rootNode = tree.root(parts[0]);

        if (rootNode == null)
            return null;

        String currentInput = input;

        do {
            BladeCommand match = rootNode.findCommandByLabel(currentInput);

            if (match != null) {
                for (String label : match.labels()) {
                    if (label.equalsIgnoreCase(currentInput)) {
                        return ResolvedCommand.match(label, match);
                    }
                }
            }

            parts = currentInput.split(" ");
            currentInput = join(" ", Arrays.asList(parts),
                0, parts.length - 1);
        } while (!currentInput.isEmpty());

        return null;
    }

    @ApiStatus.Internal
    private void addNodeRecursively(@NotNull CommandTreeNode node,
                                    @NotNull List<ResolvedCommand> into) {
        if (node.isLeaf()) {
            into.add(ResolvedCommand.match(node.label(), node.command()));
        }

        for (CommandTreeNode child : node.children().values()) {
            addNodeRecursively(child, into);
        }
    }

    /**
     * Finds the container associated with the given resolved command node.
     *
     * @param node the resolved command node
     * @param <T>  the type of container to find
     * @return the found container, or {@code null} if none is found
     */
    @Nullable
    public <T extends Container> T findContainer(@NotNull ResolvedCommand node) {
        if (node.command() != null) {
            T container = findContainer(node.command());

            if (container != null)
                return container;
        }

        for (ResolvedCommand subcommand : node.subcommands()) {
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

            CommandTreeNode node = blade.commandTree().root(realLabel);

            if (node != null && node.container() != null) {
                return (T) node.container();
            }
        }

        return null;
    }

}
