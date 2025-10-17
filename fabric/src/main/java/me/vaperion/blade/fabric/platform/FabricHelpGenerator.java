package me.vaperion.blade.fabric.platform;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.fabric.command.FabricInternalUsage;
import me.vaperion.blade.platform.HelpGenerator;
import me.vaperion.blade.util.command.PaginatedOutput;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class FabricHelpGenerator implements HelpGenerator<Text> {

    @Override
    public @NotNull List<Text> generate(@NotNull Context context, @NotNull List<BladeCommand> commands) {
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
            return List.of(
                Text.literal(context.blade().configuration().defaultPermissionMessage())
                    .formatted(Formatting.RED)
            );
        }

        return new PaginatedOutput<BladeCommand, Text>(10) {
            @Override
            public @NotNull Text error(@NotNull Error error, Object... args) {
                return switch (error) {
                    case NO_RESULTS -> Text.literal("There are no available commands matching that format.")
                        .formatted(Formatting.RED);

                    case PAGE_OUT_OF_BOUNDS -> Text.literal(String.format(
                            "Page %d does not exist, valid range is 1 to %d.", args))
                        .formatted(Formatting.RED);
                };
            }

            @Override
            public @NotNull Text header(int page, int totalPages) {
                return Text.empty()
                    .append(
                        Text.literal("==== ")
                            .formatted(Formatting.AQUA)
                    )
                    .append(
                        Text.literal("Help for /" + context.label())
                            .formatted(Formatting.YELLOW)
                    )
                    .append(
                        Text.literal(" ====")
                            .formatted(Formatting.AQUA)
                    );
            }

            @Override
            public @NotNull Text footer(int page, int totalPages) {
                return Text.empty()
                    .append(
                        Text.literal("==== ")
                            .formatted(Formatting.AQUA)
                    )
                    .append(
                        Text.literal("Page " + page + "/" + totalPages)
                            .formatted(Formatting.YELLOW)
                    )
                    .append(
                        Text.literal(" ====")
                            .formatted(Formatting.AQUA)
                    );
            }

            @Override
            public @NotNull Text line(BladeCommand result, int index) {
                Text usage = (Text) result.helpMessage().ensureGetOrLoad(
                        () -> new FabricInternalUsage(result, false))
                    .message();

                MutableText out = Text.empty()
                    .append(
                        Text.literal(" - ")
                            .formatted(Formatting.AQUA)
                    );

                usage.withoutStyle().forEach(out::append);

                if (!result.description().isEmpty()) {
                    out.append(
                        Text.literal(" - " + result.description())
                            .formatted(Formatting.GRAY)
                    );
                }

                return out;
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