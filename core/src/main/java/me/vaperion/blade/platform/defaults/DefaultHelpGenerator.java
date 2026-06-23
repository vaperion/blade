package me.vaperion.blade.platform.defaults;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.platform.api.BladeMessages;
import me.vaperion.blade.platform.api.HelpGenerator;
import me.vaperion.blade.util.command.PaginatedOutput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static me.vaperion.blade.util.BladeHelper.mergeLabelWithArgs;
import static me.vaperion.blade.util.BladeHelper.plainText;
import static net.kyori.adventure.text.Component.text;

public class DefaultHelpGenerator implements HelpGenerator {

    @Override
    public @NotNull List<Component> generate(@NotNull Context context, @NotNull List<BladeCommand> commands) {
        BladeMessages messages = context.blade().configuration().messages();

        String[] args = context.arguments();
        int page = 1;

        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[args.length - 1]);

                // Drop the last argument
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 0, newArgs, 0, args.length - 1);
                args = newArgs;
            } catch (NumberFormatException ignored) {
            }
        }

        String filterInput = mergeLabelWithArgs(context.label(), args);
        commands = filterCommands(context, commands, filterInput);

        int originalCount = commands.size();

        commands = commands.stream()
            .filter(c -> c.hasPermission(context))
            .collect(Collectors.toList());

        if (originalCount != 0 && commands.isEmpty()) {
            return Collections.singletonList(messages.noHelpPermission(context));
        }

        return new PaginatedOutput<BladeCommand>(RESULTS_PER_PAGE) {
            @Override
            public @NotNull List<Component> error(@NotNull Error error, Object... args) {
                switch (error) {
                    case NO_RESULTS:
                        return Collections.singletonList(noResults());
                    case PAGE_OUT_OF_BOUNDS:
                        return Collections.singletonList(pageOutOfBounds((int) args[0], (int) args[1]));
                }
                return Collections.singletonList(messages.error("Unknown error: " + error));
            }

            @Override
            public @NotNull List<Component> header(int page, int totalPages) {
                return Collections.singletonList(DefaultHelpGenerator.this.header(context, page, totalPages));
            }

            @Override
            public @NotNull List<Component> footer(int page, int totalPages) {
                return Collections.singletonList(DefaultHelpGenerator.this.footer(context, page, totalPages));
            }

            @Override
            public @NotNull List<Component> line(BladeCommand result, int index) {
                return Collections.singletonList(DefaultHelpGenerator.this.line(result, result.helpMessage().message()));
            }
        }.generatePage(commands, page);
    }

    @NotNull
    protected Component header(@NotNull Context context, int page, int totalPages) {
        return text()
            .append(text("==== ", NamedTextColor.AQUA))
            .append(text("Help for /" + context.label(), NamedTextColor.YELLOW))
            .append(text(" ====", NamedTextColor.AQUA))
            .asComponent();
    }

    @NotNull
    protected Component footer(@NotNull Context context, int page, int totalPages) {
        return text()
            .append(text("==== ", NamedTextColor.AQUA))
            .append(text("Page " + page + "/" + totalPages, NamedTextColor.YELLOW))
            .append(text(" ====", NamedTextColor.AQUA))
            .asComponent();
    }

    @NotNull
    protected Component noResults() {
        return text("There are no available commands matching that format.", NamedTextColor.RED);
    }

    @NotNull
    protected Component pageOutOfBounds(int page, int totalPages) {
        return text(String.format("Page %d does not exist, valid range is 1 to %d.", page, totalPages), NamedTextColor.RED);
    }

    @NotNull
    protected Component line(@NotNull BladeCommand command, @NotNull Component usage) {
        Component line = text()
            .append(text(" - ", NamedTextColor.AQUA))
            .append(text(plainText(usage), NamedTextColor.YELLOW))
            .asComponent();

        if (!command.description().isEmpty()) {
            line = line.append(text(" - " + command.description(), NamedTextColor.GRAY));
        }

        return line;
    }
}
