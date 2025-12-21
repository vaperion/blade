package me.vaperion.blade.tree;

import lombok.Getter;
import lombok.Setter;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.Container;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a node in the command tree.
 * Can be either a branch (has children) or a leaf (has a command), or both.
 */
@SuppressWarnings("unused")
@Getter
public final class CommandTreeNode {

    private final CommandTreeNode parent;
    private final String label;
    private final Map<String, CommandTreeNode> children = new ConcurrentHashMap<>();

    @Setter
    private BladeCommand command;

    @Setter
    private Container container;

    public CommandTreeNode(@Nullable CommandTreeNode parent,
                           @NotNull String label) {
        this.parent = parent;
        this.label = label;
    }

    /**
     * Checks if this node is a leaf (has an associated command).
     *
     * @return true if this node is a leaf, false otherwise
     */
    public boolean isLeaf() {
        return command != null;
    }

    /**
     * Checks if this node is a branch (has children).
     *
     * @return true if this node is a branch, false otherwise
     */
    public boolean isBranch() {
        return !children.isEmpty();
    }

    /**
     * Checks if this node is a stub (has children and no command).
     *
     * @return true if this node is a stub, false otherwise
     */
    public boolean isStub() {
        return !isLeaf() && isBranch();
    }

    /**
     * Adds a child path to this node.
     *
     * @param labels  the remaining path
     * @param command the command to associate
     */
    void addChild(@NotNull List<String> labels, @NotNull BladeCommand command) {
        if (labels.isEmpty()) {
            this.command = command;
            return;
        }

        String childLabel = labels.get(0);
        CommandTreeNode child = children.computeIfAbsent(childLabel,
            label -> new CommandTreeNode(this, label));

        if (labels.size() == 1) {
            child.command(command);
        } else {
            child.addChild(labels.subList(1, labels.size()), command);
        }
    }

    /**
     * Removes a command from a child path.
     *
     * @param labels  the remaining path
     * @param command the command to remove
     * @return true if the command was removed, false if not found
     */
    boolean removeChild(@NotNull List<String> labels, @NotNull BladeCommand command) {
        if (labels.isEmpty()) {
            if (this.command == command) {
                this.command = null;
                return true;
            }
            return false;
        }

        String childLabel = labels.get(0);
        CommandTreeNode child = children.get(childLabel);

        if (child == null) {
            return false;
        }

        boolean removed;
        if (labels.size() == 1) {
            if (child.command() == command) {
                child.command(null);
                removed = true;

                if (!child.isBranch() && !child.isLeaf()) {
                    children.remove(childLabel);
                }
            } else {
                removed = false;
            }
        } else {
            removed = child.removeChild(labels.subList(1, labels.size()), command);

            if (removed && !child.isBranch() && !child.isLeaf()) {
                children.remove(childLabel);
            }
        }

        return removed;
    }

    /**
     * Gets a child node by label.
     *
     * @param label the child label
     * @return the child node, or null if not found
     */
    @Nullable
    public CommandTreeNode child(@NotNull String label) {
        return children.get(label);
    }

    /**
     * Traverses the tree and collects all nodes.
     *
     * @return list of all nodes
     */
    @NotNull
    public List<CommandTreeNode> collectNodes() {
        List<CommandTreeNode> nodes = new ArrayList<>();
        collectNodesRecursive(nodes);
        return nodes;
    }

    private void collectNodesRecursive(@NotNull List<CommandTreeNode> into) {
        if (command != null) {
            into.add(this);
        }

        for (CommandTreeNode child : children.values()) {
            child.collectNodesRecursive(into);
        }
    }

    /**
     * Finds a command node by its full label.
     *
     * @param fullLabel the full command label
     * @return the command node if found, or null if not found
     */
    @Nullable
    public CommandTreeNode findNodeByLabel(@NotNull String fullLabel) {
        List<CommandTreeNode> nodes = collectNodes();

        for (CommandTreeNode node : nodes) {
            for (String label : node.command.labels()) {
                if (label.equalsIgnoreCase(fullLabel)) {
                    return node;
                }
            }
        }

        return null;
    }

    /**
     * Traverses the tree and collects all commands.
     *
     * @return list of all commands
     */
    @NotNull
    public List<BladeCommand> collectCommands() {
        List<BladeCommand> commands = new ArrayList<>();
        collectCommandsRecursive(commands);
        return commands;
    }

    private void collectCommandsRecursive(@NotNull List<BladeCommand> into) {
        if (command != null) {
            into.add(command);
        }

        for (CommandTreeNode child : children.values()) {
            child.collectCommandsRecursive(into);
        }
    }

    /**
     * Finds a command by its full label.
     *
     * @param fullLabel the full command label
     * @return the command if found, or null if not found
     */
    @Nullable
    public BladeCommand findCommandByLabel(@NotNull String fullLabel) {
        List<BladeCommand> commands = collectCommands();

        for (BladeCommand cmd : commands) {
            for (String label : cmd.labels()) {
                if (label.equalsIgnoreCase(fullLabel)) {
                    return cmd;
                }
            }
        }

        return null;
    }

}
