package me.vaperion.blade.tree;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.Container;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a node in the command tree.
 * Can be either a branch (has children) or a leaf (has one or more commands), or both.
 * <p>
 * A node may hold multiple commands (overloads) registered under the same label,
 * differing only in their parameters.
 */
@SuppressWarnings("unused")
@Getter
public final class CommandTreeNode {

    private final CommandTreeNode parent;
    private final String label;
    private final Map<String, CommandTreeNode> children = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final List<BladeCommand> commands = new CopyOnWriteArrayList<>();

    @Setter
    private Container container;

    public CommandTreeNode(@Nullable CommandTreeNode parent,
                           @NotNull String label) {
        this.parent = parent;
        this.label = label;
    }

    /**
     * Checks if this node is a leaf (has at least one associated command).
     *
     * @return true if this node is a leaf, false otherwise
     */
    public boolean isLeaf() {
        return !commands.isEmpty();
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

    @Nullable
    public BladeCommand command() {
        return commands.isEmpty() ? null : commands.get(0);
    }

    public void command(@Nullable BladeCommand command) {
        if (command == null) {
            commands.clear();
        } else {
            addCommand(command);
        }
    }

    @NotNull
    @Unmodifiable
    public List<BladeCommand> commands() {
        return Collections.unmodifiableList(commands);
    }

    public void addCommand(@NotNull BladeCommand command) {
        if (!commands.contains(command)) {
            commands.add(command);
        }
    }

    public boolean removeCommand(@NotNull BladeCommand command) {
        return commands.remove(command);
    }

    /**
     * Adds a child path to this node.
     *
     * @param labels  the remaining path
     * @param command the command to associate
     */
    void addChild(@NotNull List<String> labels, @NotNull BladeCommand command) {
        if (labels.isEmpty()) {
            addCommand(command);
            return;
        }

        String childLabel = labels.get(0);
        CommandTreeNode child = children.computeIfAbsent(childLabel,
            label -> new CommandTreeNode(this, label));

        if (labels.size() == 1) {
            child.addCommand(command);
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
            return removeCommand(command);
        }

        String childLabel = labels.get(0);
        CommandTreeNode child = children.get(childLabel);

        if (child == null) {
            return false;
        }

        boolean removed;
        if (labels.size() == 1) {
            removed = child.removeCommand(command);
        } else {
            removed = child.removeChild(labels.subList(1, labels.size()), command);
        }

        if (removed && !child.isBranch() && !child.isLeaf()) {
            children.remove(childLabel);
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
        if (!commands.isEmpty()) {
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
            for (BladeCommand command : node.commands) {
                for (String label : command.labels()) {
                    if (label.equalsIgnoreCase(fullLabel)) {
                        return node;
                    }
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
        into.addAll(commands);

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
