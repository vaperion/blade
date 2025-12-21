package me.vaperion.blade.impl.node;

import lombok.Getter;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.tree.CommandTreeNode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@Getter
public class ResolvedCommand {

    @NotNull
    public static ResolvedCommand match(@NotNull String label,
                                        @NotNull CommandTreeNode treeNode) {
        return new ResolvedCommand(false, label,
            treeNode, Collections.emptyList());
    }

    @NotNull
    public static ResolvedCommand stub(@NotNull List<ResolvedCommand> subcommands) {
        return new ResolvedCommand(true, null,
            null, subcommands);
    }

    private final boolean isStub;
    private final String matchedLabel;
    private final CommandTreeNode treeNode;
    private final List<ResolvedCommand> subcommands;

    public ResolvedCommand(boolean isStub,
                           @Nullable String matchedLabel,
                           @Nullable CommandTreeNode treeNode,
                           @NotNull List<ResolvedCommand> subcommands) {
        this.isStub = isStub;
        this.matchedLabel = matchedLabel;
        this.treeNode = treeNode;
        this.subcommands = subcommands;
    }

    /**
     * Returns the matched command, or {@code null} if no command was matched.
     *
     * @return The matched command or {@code null}.
     */
    @Nullable
    public BladeCommand command() {
        return treeNode != null ? treeNode.command() : null;
    }

    /**
     * Returns the matched label, or the provided default value if no label was matched.
     *
     * @param defaultValue The default value to return if no label was matched.
     * @return The matched label or the default value.
     */
    @NotNull
    public String matchedLabelOr(@NotNull String defaultValue) {
        String label = matchedLabel();
        return label != null ? label : defaultValue;
    }

    /**
     * Collects all commands from this node and its subcommands into the provided list.
     *
     * @param into The list to collect commands into.
     */
    @ApiStatus.Internal
    public void collectCommandsInto(@NotNull List<BladeCommand> into) {
        if (isStub) {
            for (ResolvedCommand node : subcommands) {
                node.collectCommandsInto(into);
            }
        } else if (treeNode != null) {
            into.add(treeNode.command());
        }
    }

    @Override
    public String toString() {
        if (isStub) {
            return "ResolvedCommandNode{STUB, subcommands=" + subcommands.size() + "}";
        }

        String cmdString = treeNode == null
            ? "null"
            : treeNode.command().mainLabel();

        return "ResolvedCommandNode{MATCHED, label='" + matchedLabel + "', command=" + cmdString + "}";
    }
}
