package me.vaperion.blade.velocity.platform;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.platform.HelpGenerator;
import me.vaperion.blade.util.command.PaginatedOutput;
import me.vaperion.blade.velocity.command.VelocityInternalUsage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;

public class VelocityHelpGenerator implements HelpGenerator<Component> {

    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])");

    @Override
    public @NotNull List<Component> generate(@NotNull Context context, @NotNull List<BladeCommand> commands) {
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
            return Collections.singletonList(
                text(context.blade().configuration().defaultPermissionMessage(),
                    NamedTextColor.RED)
            );
        }

        return new PaginatedOutput<BladeCommand, Component>(10) {
            @Override
            public @NotNull Component error(@NotNull Error error, Object... args) {
                switch (error) {
                    case NO_RESULTS:
                        return text("There are no available commands matching that format.", NamedTextColor.RED);

                    case PAGE_OUT_OF_BOUNDS:
                        return text(String.format(
                                "Page %d does not exist, valid range is 1 to %d.", args),
                            NamedTextColor.RED);
                }
                ;

                return text(String.format("Unknown error: %s",
                    error), NamedTextColor.RED);
            }

            @Override
            public @NotNull Component header(int page, int totalPages) {
                return text()
                    .append(
                        text("==== ", NamedTextColor.AQUA)
                    )
                    .append(
                        text("Help for /" + context.label(), NamedTextColor.YELLOW)
                    )
                    .append(
                        text(" ====", NamedTextColor.AQUA)
                    )
                    .build();
            }

            @Override
            public @NotNull Component footer(int page, int totalPages) {
                return text()
                    .append(
                        text("==== ", NamedTextColor.AQUA)
                    )
                    .append(
                        text("Page " + page + "/" + totalPages, NamedTextColor.YELLOW)
                    )
                    .append(
                        text(" ====", NamedTextColor.AQUA)
                    )
                    .build();
            }

            @Override
            public @NotNull Component line(BladeCommand result, int index) {
                Component usage = (Component) result.helpMessage().ensureGetOrLoad(
                        () -> new VelocityInternalUsage(result, false))
                    .message();

                TextComponent.Builder out = text()
                    .append(
                        text(" - ", NamedTextColor.AQUA)
                    )
                    .append(
                        usage.style(Style.style().build())
                    );

                if (!result.description().isEmpty()) {
                    out.append(
                        text(" - " + result.description(), NamedTextColor.GRAY)
                    );
                }

                return out.build();
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