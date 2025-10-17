package me.vaperion.blade.bukkit.platform;

import me.vaperion.blade.bukkit.command.BukkitInternalUsage;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.platform.HelpGenerator;
import me.vaperion.blade.util.command.PaginatedOutput;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BukkitHelpGenerator implements HelpGenerator<String> {

    @NotNull
    @Override
    public List<String> generate(@NotNull Context context, @NotNull List<BladeCommand> commands) {
        commands = commands.stream()
            .distinct()
            .filter(c -> !c.hidden())
            .sorted(context.blade().configuration().helpSorter())
            .collect(Collectors.toList());

        int originalCount = commands.size();
        commands = commands.stream()
            .filter(c -> c.hasPermission(context))
            .collect(Collectors.toList());

        if (originalCount != 0 && commands.isEmpty()) {
            return Collections.singletonList(ChatColor.RED + context.blade().configuration().defaultPermissionMessage());
        }

        return new PaginatedOutput<BladeCommand, String>(10) {
            @Override
            public @NotNull String error(@NotNull Error error, Object... args) {
                switch (error) {
                    case NO_RESULTS:
                        return ChatColor.RED + "There are no available commands matching that format.";
                    case PAGE_OUT_OF_BOUNDS:
                        return ChatColor.RED + String.format("Page %d does not exist, valid range is 1 to %d.", args);
                }
                return ChatColor.RED + String.format("Unknown error %s", error.name());
            }

            @Override
            public @NotNull String header(int page, int totalPages) {
                return ChatColor.AQUA + "==== " + ChatColor.YELLOW + "Help for /" + context.label() + ChatColor.AQUA + " ====";
            }

            @Override
            public @NotNull String footer(int page, int totalPages) {
                return ChatColor.AQUA + "==== " + ChatColor.YELLOW + "Page " + page + "/" + totalPages + ChatColor.AQUA + " ====";
            }

            @Override
            public @NotNull String line(BladeCommand result, int index) {
                String help = (String) result.helpMessage()
                    .ensureGetOrLoad(() -> new BukkitInternalUsage(result, false))
                    .message();

                return ChatColor.AQUA + " - " + ChatColor.YELLOW + ChatColor.stripColor(help) +
                    (result.description().isEmpty() ? "" : (" - " + ChatColor.GRAY + result.description()));
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