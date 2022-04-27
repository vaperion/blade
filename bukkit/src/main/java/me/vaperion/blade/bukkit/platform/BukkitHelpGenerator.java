package me.vaperion.blade.bukkit.platform;

import me.vaperion.blade.bukkit.command.BukkitUsageMessage;
import me.vaperion.blade.command.Command;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.platform.HelpGenerator;
import me.vaperion.blade.util.PaginatedOutput;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class BukkitHelpGenerator implements HelpGenerator {

    @NotNull
    @Override
    public List<String> generate(@NotNull Context context, @NotNull List<Command> commands) {
        commands = commands.stream().distinct().filter(c -> !c.isHidden()).collect(Collectors.toList());

        return new PaginatedOutput<Command>(10) {
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
            public String formatLine(Command result, int index) {
                String help = ChatColor.stripColor(result.getHelpMessage().ensureGetOrLoad(() -> new BukkitUsageMessage(result, false)).toString());
                return ChatColor.AQUA + " - " + ChatColor.YELLOW + help + (result.getDescription().isEmpty() ? "" : (" - " + ChatColor.GRAY + result.getDescription()));
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