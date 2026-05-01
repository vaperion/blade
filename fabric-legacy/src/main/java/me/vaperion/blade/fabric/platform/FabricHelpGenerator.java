package me.vaperion.blade.fabric.platform;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.fabric.util.TextUtil;
import me.vaperion.blade.platform.api.HelpGenerator;
import me.vaperion.blade.util.command.PaginatedOutput;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static me.vaperion.blade.util.BladeHelper.mergeLabelWithArgs;

public class FabricHelpGenerator implements HelpGenerator<Component> {

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
                Component.literal(context.blade().configuration().defaultPermissionMessage())
                    .withStyle(ChatFormatting.RED)
            );
        }

        return new PaginatedOutput<BladeCommand, Component>(RESULTS_PER_PAGE) {
            @Override
            public @NotNull Component error(@NotNull Error error, Object... args) {
                return switch (error) {
                    case NO_RESULTS -> Component.literal("There are no available commands matching that format.")
                        .withStyle(ChatFormatting.RED);

                    case PAGE_OUT_OF_BOUNDS -> Component.literal(String.format(
                            "Page %d does not exist, valid range is 1 to %d.", args))
                        .withStyle(ChatFormatting.RED);
                };
            }

            @Override
            public @NotNull Component header(int page, int totalPages) {
                return Component.empty()
                    .append(
                        Component.literal("==== ")
                            .withStyle(ChatFormatting.AQUA)
                    )
                    .append(
                        Component.literal("Help for /" + context.label())
                            .withStyle(ChatFormatting.YELLOW)
                    )
                    .append(
                        Component.literal(" ====")
                            .withStyle(ChatFormatting.AQUA)
                    );
            }

            @Override
            public @NotNull Component footer(int page, int totalPages) {
                return Component.empty()
                    .append(
                        Component.literal("==== ")
                            .withStyle(ChatFormatting.AQUA)
                    )
                    .append(
                        Component.literal("Page " + page + "/" + totalPages)
                            .withStyle(ChatFormatting.YELLOW)
                    )
                    .append(
                        Component.literal(" ====")
                            .withStyle(ChatFormatting.AQUA)
                    );
            }

            @Override
            public @NotNull Component line(BladeCommand result, int index) {
                Component usage = (Component) result.helpMessage().message();

                MutableComponent out = Component.empty()
                    .append(
                        Component.literal(" - ")
                            .withStyle(ChatFormatting.AQUA)
                    );

                out.append(
                    Component.literal(TextUtil.toRaw(usage))
                        .withStyle(ChatFormatting.YELLOW)
                );

                if (!result.description().isEmpty()) {
                    out.append(
                        Component.literal(" - " + result.description())
                            .withStyle(ChatFormatting.GRAY)
                    );
                }

                return out;
            }
        }.generatePage(commands, page);
    }
}
