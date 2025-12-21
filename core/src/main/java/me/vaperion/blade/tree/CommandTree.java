package me.vaperion.blade.tree;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the root of a command tree structure.
 */
@SuppressWarnings({ "unused", "UnusedReturnValue" })
@Getter
@RequiredArgsConstructor
public final class CommandTree {

    private final Blade blade;
    private final Map<String, CommandTreeNode> roots = new ConcurrentHashMap<>();

    /**
     * Adds a command to the tree.
     *
     * @param labels  the command path (e.g., ["a", "b", "c"] for command "a b c")
     * @param command the command to associate with this path
     */
    public void addCommand(@NotNull List<String> labels, @NotNull BladeCommand command) {
        if (labels.isEmpty()) {
            throw new IllegalArgumentException("Labels cannot be empty");
        }

        String rootLabel = labels.get(0);

        CommandTreeNode root = roots.computeIfAbsent(rootLabel,
            label -> new CommandTreeNode(null, label));

        if (root.container() == null) {
            try {
                root.container(
                    blade.platform().containerCreator(command).create(blade, rootLabel)
                );
            } catch (Throwable t) {
                blade.logger().error(t, "Failed to create command container for root label: " + rootLabel);
            }
        }

        if (labels.size() == 1) {
            root.command(command);
        } else {
            root.addChild(labels.subList(1, labels.size()), command);
        }
    }

    /**
     * Removes a command from the tree.
     *
     * @param labels  the command path (e.g., ["a", "b", "c"] for command "a b c")
     * @param command the command to remove
     * @return true if the command was removed, false if not found
     */
    public boolean removeCommand(@NotNull List<String> labels, @NotNull BladeCommand command) {
        if (labels.isEmpty()) {
            return false;
        }

        String rootLabel = labels.get(0);
        CommandTreeNode root = roots.get(rootLabel);

        if (root == null) {
            return false;
        }

        if (labels.size() == 1) {
            if (root.command() == command) {
                root.command(null);

                if (!root.isBranch() && !root.isLeaf()) {
                    roots.remove(rootLabel);
                }

                return true;
            }
            return false;
        }

        boolean removed = root.removeChild(labels.subList(1, labels.size()), command);

        if (removed && !root.isBranch() && !root.isLeaf()) {
            roots.remove(rootLabel);

            //noinspection StatementWithEmptyBody
            if (root.container() != null) {
                // unregister container somehow?
            }
        }

        return removed;
    }

    /**
     * Gets a root node by label.
     *
     * @param label the root label
     * @return the root node, or null if not found
     */
    @Nullable
    public CommandTreeNode root(@NotNull String label) {
        return roots.get(label);
    }

    /**
     * Finds a command node by traversing the path.
     *
     * @param path the path to traverse (e.g., ["a", "b", "c"] for command "a b c")
     * @return the node at the path, or null if not found
     */
    @Nullable
    public CommandTreeNode findNode(@NotNull List<String> path) {
        if (path.isEmpty()) return null;

        CommandTreeNode current = roots.get(path.get(0));
        if (current == null) return null;

        for (int i = 1; i < path.size(); i++) {
            current = current.child(path.get(i));
            if (current == null) return null;
        }

        return current;
    }
}
