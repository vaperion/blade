package me.vaperion.blade.util;

import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.command.Command;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.context.WrappedSender;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.sender.internal.SndProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unused")
public interface BladeHelper {

    @ApiStatus.Internal
    @NotNull
    static List<Object> makeMethodArguments(@NotNull Blade blade,
                                            @NotNull BladeCommand command,
                                            @NotNull Context context,
                                            @NotNull String[] rawArgs,
                                            @NotNull Object rawSender)
        throws BladeExitMessage {
        if (command.isContextBased()) {
            // If the command is context-based, we only need to pass the context
            return Collections.singletonList(context);
        }

        List<Object> parsed = blade.getParser()
            .parseArguments(command, context, rawArgs);

        if (command.isHasSenderParameter()) {
            // If the command has a sender parameter, we need to add the sender

            if (command.isWrappedSenderBased()) {
                // Generic blade wrapped sender

                parsed.add(0, context.sender());
            } else {
                // Raw sender, this is usually a platform type like Player, CommandSender, etc.
                // However, argument providers are also supported to extract custom sender types.

                Object sender = parseSender(blade,
                    command.getSenderProviders(),
                    context,
                    context.sender());

                if (sender == null && command.getSenderType().isInstance(rawSender)) {
                    // If the sender is not provided by any provider, and the raw sender
                    // is of the expected type, we can use it directly.

                    sender = rawSender;
                }

                if (sender == null) {
                    // This error message is not ideal, but it's better to give some context.

                    throw new BladeExitMessage("This command can only be executed by " +
                        command.getSenderType().getSimpleName().toLowerCase(Locale.ROOT) + "s!");
                }

                parsed.add(0, sender);
            }
        }

        return parsed;
    }

    @ApiStatus.Internal
    @Nullable
    static Object parseSender(@NotNull Blade blade,
                              @NotNull List<SndProvider<?>> providers,
                              @NotNull Context context,
                              @NotNull WrappedSender<?> sender) {
        for (SndProvider<?> provider : providers) {
            Object result = provider.getProvider().provide(context, sender);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    @ApiStatus.Internal
    @NotNull
    static List<String> generateAliases(@NotNull Command command,
                                        @Nullable Command parentCommand) {
        if (parentCommand == null)
            return Arrays.asList(command.value());

        List<String> aliases = new ArrayList<>();

        for (String alias : command.value()) {
            for (String parentAlias : parentCommand.value()) {
                String fullAlias = parentAlias + " " + alias;
                aliases.add(fullAlias.trim().toLowerCase());
            }
        }

        return aliases;
    }

}
