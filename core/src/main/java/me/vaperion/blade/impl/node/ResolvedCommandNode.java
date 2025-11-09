package me.vaperion.blade.impl.node;

import lombok.Getter;
import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@Getter
public class ResolvedCommandNode {

    @NotNull
    public static ResolvedCommandNode match(@NotNull String label,
                                            @NotNull BladeCommand command) {
        return new ResolvedCommandNode(false, label,
            command, Collections.emptyList());
    }

    @NotNull
    public static ResolvedCommandNode stub(@NotNull List<ResolvedCommandNode> nodes) {
        return new ResolvedCommandNode(true, null,
            null, nodes);
    }

    private final boolean isStub;
    private final String matchedLabel;
    private final BladeCommand command;
    private final List<ResolvedCommandNode> subcommands;

    public ResolvedCommandNode(boolean isStub,
                               @Nullable String matchedLabel,
                               @Nullable BladeCommand command,
                               @NotNull List<ResolvedCommandNode> subcommands) {
        this.isStub = isStub;
        this.matchedLabel = matchedLabel;
        this.command = command;
        this.subcommands = subcommands;
    }

    /**
     * Returns the matched label, or the provided default value if no label was matched.
     *
     * @param defaultValue The default value to return if no label was matched.
     * @return The matched label or the default value.
     */
    @NotNull
    public String matchedLabelOr(@NotNull String defaultValue) {
        return matchedLabel != null ? matchedLabel : defaultValue;
    }

    /**
     * Collects all commands from this node and its subcommands into the provided list.
     *
     * @param into The list to collect commands into.
     */
    @ApiStatus.Internal
    public void collectCommandsInto(@NotNull List<BladeCommand> into) {
        if (isStub) {
            for (ResolvedCommandNode node : subcommands) {
                node.collectCommandsInto(into);
            }
        } else if (command != null) {
            into.add(command);
        }
    }

    @Override
    public String toString() {
        if (isStub) {
            return "ResolvedCommandNode{STUB, subcommands=" + subcommands.size() + "}";
        }

        String cmdString = command == null
            ? "null"
            : command.mainLabel();

        return "ResolvedCommandNode{MATCHED, label='" + matchedLabel + "', command=" + cmdString + "}";
    }
}
