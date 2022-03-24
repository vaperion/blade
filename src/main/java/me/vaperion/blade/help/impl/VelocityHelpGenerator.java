package me.vaperion.blade.help.impl;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.impl.VelocityUsageMessage;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.help.HelpGenerator;
import me.vaperion.blade.utils.PaginatedOutput;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VelocityHelpGenerator implements HelpGenerator {

    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])");

    @Override
    public @NotNull List<String> generate(@NotNull BladeContext context, @NotNull List<BladeCommand> commands) {
        commands = commands.stream().distinct().filter(c -> !c.isHidden()).collect(Collectors.toList());

        return new PaginatedOutput<BladeCommand>(10) {
            @Override
            public String formatErrorMessage(Error error, Object... args) {
                switch (error) {
                    case NO_RESULTS:
                        return "&cNo results found.";
                    case PAGE_OUT_OF_BOUNDS:
                        return String.format("&cPage %d does not exist, valid range is 1 to %d.", args);
                }
                return null;
            }

            @Override
            public String getHeader(int page, int totalPages) {
                return "&b==== &eHelp for /" + context.alias() + "&b ====";
            }

            @Override
            public String getFooter(int page, int totalPages) {
                return "&b==== &ePage " + page + "/" + totalPages + "&b ====";
            }

            @Override
            public String formatLine(BladeCommand result, int index) {
                return "&b - &e" + stripColor(result.getUsageMessage().ensureGetOrLoad(() -> new VelocityUsageMessage(result)).toString().replace("Usage: ", "")) +
                      (result.getDescription().isEmpty() ? "" : (" - &7" + result.getDescription()));
            }
        }.generatePage(commands, parsePage(context.argument(0)));
    }

    private String stripColor(String string) {
        return COLOR_PATTERN.matcher(string).replaceAll("");
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
