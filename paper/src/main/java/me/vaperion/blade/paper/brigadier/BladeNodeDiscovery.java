package me.vaperion.blade.paper.brigadier;

import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.util.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BladeNodeDiscovery {

    private final Blade blade;

    public BladeNodeDiscovery(@NotNull Blade blade) {
        this.blade = blade;
    }

    @NotNull
    private String removeCommandQualifier(@NotNull String input) {
        String[] parts = input.split(" ");

        if (parts[0].contains(":"))
            parts[0] = parts[0].split(":")[1];

        return String.join(" ", parts);
    }

    @SuppressWarnings("ReplaceNullCheck")
    @Nullable
    public SimpleBladeNode discoverCommand(@NotNull String label) {
        label = removeCommandQualifier(label);
        Tuple<BladeCommand, String> bladeCommand = blade.getResolver().resolveCommand(new String[]{ label });

        if (bladeCommand != null) {
            // This is the simple case: if a command is registered with that exact label (e.g. "/hello"), we can just return it
            return new SimpleBladeNode(false, bladeCommand.getLeft(), List.of());
        }

        // If no command was found, we have to search for "stub" parent commands, e.g. if you register "/hello world", "hello" would be a stub
        List<BladeCommand> commands = blade.getAliasToCommands().get(label);
        if (commands == null || commands.isEmpty()) return null;

        List<SimpleBladeNode> resolved = new ArrayList<>();
        for (BladeCommand command : commands) {
            SimpleBladeNode subcommand = discoverCommand(command.getAliases()[0]);

            if (subcommand == null)
                resolved.add(new SimpleBladeNode(false, command, List.of()));
            else
                resolved.add(subcommand);
        }

        return new SimpleBladeNode(true, null, resolved);
    }
}
