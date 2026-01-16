package me.vaperion.blade.hytale.platform;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.util.MessageUtil;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.platform.api.HelpGenerator;
import me.vaperion.blade.util.command.PaginatedOutput;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.hypixel.hytale.server.core.Message.raw;
import static me.vaperion.blade.util.BladeHelper.mergeLabelWithArgs;

public class HytaleHelpGenerator implements HelpGenerator<Message> {

    @Override
    public @NotNull List<Message> generate(@NotNull Context context, @NotNull List<BladeCommand> commands) {
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
            return Collections.singletonList(
                raw(context.blade().configuration().defaultPermissionMessage()).color(Color.RED)
            );
        }

        return new PaginatedOutput<BladeCommand, Message>(RESULTS_PER_PAGE) {
            @Override
            public @NotNull Message error(@NotNull Error error, Object... args) {
                switch (error) {
                    case NO_RESULTS:
                        return raw("There are no available commands matching that format.")
                            .color(Color.RED);

                    case PAGE_OUT_OF_BOUNDS:
                        return raw(String.format(
                            "Page %d does not exist, valid range is 1 to %d.",
                            args)).color(Color.RED);
                }

                return raw("Unknown error: " + error)
                    .color(Color.RED);
            }

            @Override
            public @NotNull Message header(int page, int totalPages) {
                Message message = Message.empty();

                message.insert(
                    raw("==== ").color(Color.CYAN)
                );

                message.insert(
                    raw("Help for /" + context.label()).color(Color.YELLOW)
                );

                message.insert(
                    raw(" ====").color(Color.CYAN)
                );

                return message;
            }

            @Override
            public @NotNull Message footer(int page, int totalPages) {
                Message message = Message.empty();

                message.insert(
                    raw("==== ").color(Color.CYAN)
                );

                message.insert(
                    raw("Page " + page + "/" + totalPages).color(Color.YELLOW)
                );

                message.insert(
                    raw(" ====").color(Color.CYAN)
                );

                return message;
            }

            @Override
            public @NotNull Message line(BladeCommand result, int index) {
                Message usage = (Message) result.helpMessage().message();

                Message message = Message.empty();

                message.insert(
                    raw(" - ").color(Color.CYAN)
                );

                message.insert(toRaw(message));

                if (!result.description().isEmpty()) {
                    message.insert(
                        raw(" - " + result.description()).color(Color.GRAY)
                    );
                }

                return message;
            }
        }.generatePage(commands, page);
    }

    @NotNull
    private static String toRaw(@NotNull Message message) {
        StringBuilder sb = new StringBuilder();

        FormattedMessage formattedMessage = message.getFormattedMessage();

        if (formattedMessage.rawText != null) {
            sb.append(formattedMessage.rawText);
        } else if (formattedMessage.messageId != null) {
            String msg = I18nModule.get().getMessage("en-US", formattedMessage.messageId);

            if (msg != null) {
                sb.append(MessageUtil.formatText(msg,
                    formattedMessage.params,
                    formattedMessage.messageParams));
            }
        }

        for (Message child : message.getChildren()) {
            sb.append(toRaw(child));
        }

        return sb.toString();
    }

}
