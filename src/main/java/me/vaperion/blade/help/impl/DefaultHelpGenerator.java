package me.vaperion.blade.help.impl;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.help.HelpGenerator;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DefaultHelpGenerator implements HelpGenerator {

    private static final String LINE = ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "------------------------------";

    @NotNull
    @Override
    public List<String> generate(@NotNull BladeContext context, @NotNull List<BladeCommand> commands) {
        commands = commands.stream().distinct().filter(c -> !c.isHidden()).collect(Collectors.toList());
        List<String> lines = new ArrayList<>();

        if (commands.isEmpty()) {
            lines.add(ChatColor.RED + context.commandService().getDefaultPermissionMessage());
            return lines;
        }

        lines.add(LINE);

        for (BladeCommand command : commands) {
            String cmd = Arrays.stream(command.getAliases())
                  .filter(a -> a.toLowerCase(Locale.ROOT).startsWith(context.alias().toLowerCase(Locale.ROOT)))
                  .findFirst().orElse(null);
            if (cmd == null) continue;
            lines.add(ChatColor.AQUA + "/" + cmd);
        }

        lines.add(LINE);

        return lines;
    }
}
