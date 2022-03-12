package me.vaperion.blade.help.impl;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.impl.BukkitUsageMessage;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.help.HelpGenerator;
import me.vaperion.blade.utils.PaginatedOutput;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class BukkitHelpGenerator implements HelpGenerator {

    @NotNull
    @Override
    public List<String> generate(@NotNull BladeContext context, @NotNull List<BladeCommand> commands) {
        commands = commands.stream().distinct().filter(c -> !c.isHidden()).collect(Collectors.toList());

        return new PaginatedOutput<BladeCommand>(10) {
            @Override
            public String formatErrorMessage(Error error, Object... args) {
                switch (error) {
                    case NO_RESULTS:
                        return ChatColor.RED + "No results found.";
                    case PAGE_OUT_OF_BOUNDS:
                        return ChatColor.RED + String.format("Page %d does not exist, valid range is 1 to %d.", args);
                }
                return null;
            }

            @Override
            public String getHeader(int page, int totalPages) {
                return ChatColor.AQUA + "==== " + ChatColor.YELLOW + "Help for /" + context.alias() + ChatColor.AQUA + " ====";
            }

            @Override
            public String getFooter(int page, int totalPages) {
                return ChatColor.AQUA + "==== " + ChatColor.YELLOW + "Page " + page + "/" + totalPages + ChatColor.AQUA + " ====";
            }

            @Override
            public String formatLine(BladeCommand result, int index) {
                return ChatColor.AQUA + " - " +
                      ChatColor.YELLOW + ChatColor.stripColor(result.getUsageMessage().ensureGetOrLoad(() -> new BukkitUsageMessage(result)).toString().replace("Usage: ", "")) +
                      (result.getDescription().isEmpty() ? "" : (" - " + ChatColor.GRAY + result.getDescription()));
            }
        }.generatePage(commands, parsePage(context.argument(0)));
    }

    private int parsePage(String argument) {
        if (argument == null) return 1;
        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
