package me.vaperion.blade.minestom.platform;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.platform.api.HelpGenerator;
import me.vaperion.blade.util.command.PaginatedOutput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static me.vaperion.blade.util.BladeHelper.mergeLabelWithArgs;
import static net.kyori.adventure.text.Component.text;

public class MinestomHelpGenerator implements HelpGenerator<Component> {

    @Override
    public @NotNull List<Component> generate(@NotNull Context context, @NotNull List<BladeCommand> commands) {
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
            return List.of(
                text(context.blade().configuration().defaultPermissionMessage(),
                    NamedTextColor.RED)
            );
        }

        return new PaginatedOutput<BladeCommand, Component>(RESULTS_PER_PAGE) {
            @Override
            public @NotNull List<Component> error(@NotNull Error error, Object... args) {
                switch (error) {
                    case NO_RESULTS:
                        return List.of(text("There are no available commands matching that format.", NamedTextColor.RED));

                    case PAGE_OUT_OF_BOUNDS:
                        return List.of(text(String.format("Page %d does not exist, valid range is 1 to %d.", args), NamedTextColor.RED));
                }

                return List.of(text(String.format("Unknown error: %s", error), NamedTextColor.RED));
            }

            @Override
            public @NotNull List<Component> header(int page, int totalPages) {
                return List.of(text()
                    .append(
                        text("==== ", NamedTextColor.AQUA)
                    )
                    .append(
                        text("Help for /" + context.label(), NamedTextColor.YELLOW)
                    )
                    .append(
                        text(" ====", NamedTextColor.AQUA)
                    )
                    .asComponent());
            }

            @Override
            public @NotNull List<Component> footer(int page, int totalPages) {
                return List.of(text()
                    .append(
                        text("==== ", NamedTextColor.AQUA)
                    )
                    .append(
                        text("Page " + page + "/" + totalPages, NamedTextColor.YELLOW)
                    )
                    .append(
                        text(" ====", NamedTextColor.AQUA)
                    )
                    .asComponent());
            }

            @Override
            public @NotNull List<Component> line(BladeCommand result, int index) {
                Component usage = (Component) result.helpMessage().message();

                TextComponent.Builder out = text()
                    .append(
                        text(" - ", NamedTextColor.AQUA)
                    );

                out.append(
                    text(toRaw(usage), NamedTextColor.YELLOW)
                );

                if (!result.description().isEmpty()) {
                    out.append(
                        text(" - " + result.description(), NamedTextColor.GRAY)
                    );
                }

                return List.of(out.asComponent());
            }
        }.generatePage(commands, page);
    }

    @SuppressWarnings("ConstantValue")
    @NotNull
    private static String toRaw(@NotNull Component component) {
        if (component == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (component instanceof TextComponent tc) {
            if (!tc.content().isEmpty()) {
                sb.append(tc.content());
            }
        }

        for (Component child : component.children()) {
            if (child == null) continue;
            sb.append(toRaw(child));
        }

        return sb.toString();
    }

}
