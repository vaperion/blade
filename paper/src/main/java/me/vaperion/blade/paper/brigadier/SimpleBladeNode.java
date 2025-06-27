package me.vaperion.blade.paper.brigadier;

import lombok.Getter;
import lombok.ToString;
import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
@Getter
@ToString
public class SimpleBladeNode {

    private final boolean isStub;
    private final BladeCommand command;
    private final List<SimpleBladeNode> subCommands;

    public SimpleBladeNode(boolean isStub, @Nullable BladeCommand command, @NotNull List<SimpleBladeNode> subCommands) {
        this.isStub = isStub;
        this.command = command;
        this.subCommands = subCommands;
    }
}
