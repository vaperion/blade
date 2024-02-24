package me.vaperion.blade.bukkit.brigadier;

import lombok.Getter;
import lombok.ToString;
import me.vaperion.blade.command.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@ToString
public class SimpleBladeNode {

    private final boolean isStub;
    private final Command command;
    private final List<SimpleBladeNode> subCommands;

    public SimpleBladeNode(boolean isStub, @Nullable Command command, @NotNull List<SimpleBladeNode> subCommands) {
        this.isStub = isStub;
        this.command = command;
        this.subCommands = subCommands;
    }
}
