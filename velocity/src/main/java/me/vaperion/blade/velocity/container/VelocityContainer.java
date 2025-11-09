package me.vaperion.blade.velocity.container;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.Container;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.exception.internal.BladeFatalError;
import me.vaperion.blade.exception.internal.BladeInternalError;
import me.vaperion.blade.exception.internal.BladeInvocationError;
import me.vaperion.blade.impl.node.ResolvedCommand;
import me.vaperion.blade.impl.suggestions.SuggestionType;
import me.vaperion.blade.tokenizer.TokenizerError;
import me.vaperion.blade.tokenizer.input.CommandInput;
import me.vaperion.blade.tokenizer.input.InputOption;
import me.vaperion.blade.util.ErrorMessage;
import me.vaperion.blade.velocity.BladeVelocityPlatform;
import me.vaperion.blade.velocity.command.VelocityInternalUsage;
import me.vaperion.blade.velocity.context.VelocitySender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.vaperion.blade.util.BladeHelper.*;
import static net.kyori.adventure.text.Component.text;

@Getter
public class VelocityContainer implements RawCommand, Container {

    public static final ContainerCreator<VelocityContainer> CREATOR = VelocityContainer::new;

    private static final Component UNKNOWN_COMMAND_MESSAGE = text(
        "Unknown command. Type \"/help\" for help."
    );

    private final Blade blade;

    private VelocityContainer(@NotNull Blade blade, @NotNull String label) {
        this.blade = blade;

        ProxyServer proxyServer = blade.platformAs(BladeVelocityPlatform.class).server();
        CommandManager commandManager = proxyServer.getCommandManager();

        CommandMeta meta = commandManager.metaBuilder(label).build();
        commandManager.register(meta, this);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // Permission check is done in the execute method, as we don't know the exact command here.
        return true;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();

        String[] args = invocation.arguments().isEmpty()
            ? new String[0]
            : invocation.arguments().split(" ");
        String label = invocation.alias();

        String commandLine = mergeLabelWithArgs(label, args);

        ResolvedCommand node = blade.nodeResolver().resolve(
            commandLine
        );

        if (node == null) {
            sender.sendMessage(UNKNOWN_COMMAND_MESSAGE);

            if (blade.configuration().verbose())
                blade.logger().info(
                    "%s tried to execute unknown command: `%s`. This is most likely a bug in Blade, not your plugin. Please report it.",
                    sender.toString(),
                    commandLine
                );
            return;
        }

        Context context = new Context(
            blade,
            new VelocitySender(sender),
            node.matchedLabelOr(label),
            args
        );

        if (node.isStub() || node.command() == null) {
            sendHelpMessage(sender,
                context,
                node.subcommands());
            return;
        }

        BladeCommand command = node.command();

        if (!command.hasPermission(context)) {
            sender.sendMessage(text(command.permissionMessage(), NamedTextColor.RED));
            return;
        }

        try {
            Runnable runnable = () -> {
                try {
                    CommandInput input = command.tokenize(
                        context.sender(),
                        "/" + removeCommandQualifier(commandLine)
                    );

                    if (!input.mergeTokensToFormWholeLabel(node.matchedLabel())) {
                        // Failed to merge label - can't execute command.
                        throw new BladeFatalError("Failed to parse command input for execution.");
                    }

                    ErrorMessage error = blade.executor().execute(context, input, node);

                    if (error != null) {
                        switch (error.type()) {
                            case LINES:
                                for (String line : error.lines()) {
                                    sender.sendMessage(text(line, NamedTextColor.RED));
                                }
                                break;

                            case SHOW_COMMAND_USAGE:
                                command.usageMessage().ensureGetOrLoad(
                                    () -> new VelocityInternalUsage(command, true)
                                ).sendTo(context);
                                break;
                        }
                    }
                } catch (BladeParseError | BladeFatalError e) {
                    sender.sendMessage(
                        text(e.getMessage(), NamedTextColor.RED)
                    );
                } catch (BladeInvocationError e) {
                    sender.sendMessage(
                        text(ERROR_MESSAGE, NamedTextColor.RED)
                    );

                    blade.logger().error(e, "Blade failed to invoke the method for command `%s` executed by %s. This is most likely a bug in your plugin.",
                        label, sender.toString());
                } catch (BladeInternalError e) {
                    sender.sendMessage(
                        text(ERROR_MESSAGE, NamedTextColor.RED)
                    );
                    command.usageMessage().ensureGetOrLoad(
                        () -> new VelocityInternalUsage(command, true)
                    ).sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                        sender.toString(), label);
                } catch (TokenizerError error) {
                    sender.sendMessage(
                        text(error.formatForChat(), NamedTextColor.RED)
                    );

                    if (!error.type().isSilent()) {
                        blade.logger().error(
                            "Failed to parse %s's command input for command `%s`: %s",
                            sender.toString(),
                            label, TokenizerError.generateFancyMessage(error));
                    }
                } catch (Throwable t) {
                    blade.logger().error(t, "An unexpected error occurred while %s was executing the command `%s`.",
                        sender.toString(), label);
                }
            };

            if (command.async()) {
                blade.configuration().asyncExecutor().accept(runnable);
            } else {
                long time = System.nanoTime();
                runnable.run();
                long elapsed = (System.nanoTime() - time) / 1000000;

                if (elapsed >= blade.configuration().executionTimeWarningThreshold()) {
                    blade.logger().warn(
                        "Command `%s` (%s#%s) took %d milliseconds to execute!",
                        command.mainLabel(),
                        command.method().getDeclaringClass().getName(),
                        command.method().getName(),
                        elapsed
                    );
                }
            }
        } catch (Throwable t) {
            blade.logger().error(t, "An unexpected error occurred while %s was executing the command `%s`.",
                sender.toString(), label);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments().split(" ");
        String label = invocation.alias();

        if (!blade.configuration().tabCompleter().isDefault())
            return Collections.emptyList();

        String commandLine = mergeLabelWithArgs(label, args);

        ResolvedCommand node = blade.nodeResolver().resolve(
            commandLine
        );

        if (node == null) {
            // No main command and not a stub either - not a blade command at all?
            return Collections.emptyList();
        }

        try {
            if (!node.isStub()) {
                // Found exact command, we can suggest arguments here.

                Context context = new Context(
                    blade,
                    new VelocitySender(sender),
                    node.matchedLabel(),
                    args
                );

                CommandInput input = node.command().tokenize(
                    context.sender(),
                    "/" + removeCommandQualifier(commandLine)
                );

                if (!input.mergeTokensToFormWholeLabel(node.matchedLabel())) {
                    // Failed to merge label - can't suggest arguments.
                    throw new BladeFatalError("Failed to parse command input for tab completion.");
                }

                return blade.suggestionProvider().suggest(
                    context,
                    input,
                    SuggestionType.ARGUMENTS
                );
            }

            // Only found command stub - suggest subcommands.

            Context context = new Context(
                blade,
                new VelocitySender(sender),
                "",
                args
            );

            CommandInput input = new CommandInput(
                blade,
                null,
                "/" + removeCommandQualifier(commandLine),
                InputOption.DISALLOW_FLAGS
            );

            input.tokenize();

            return blade.suggestionProvider().suggest(
                context,
                input,
                SuggestionType.SUBCOMMANDS
            );
        } catch (BladeInternalError e) {
            sender.sendMessage(
                text(ERROR_MESSAGE, NamedTextColor.RED)
            );

            blade.logger().error(e, "An error occurred while %s was tab completing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                sender.toString(), label);
        } catch (BladeFatalError ex) {
            sender.sendMessage(
                text(ex.getMessage(), NamedTextColor.RED)
            );
        } catch (TokenizerError error) {
            // Don't send tokenizer errors to the user during tab completion - just log them.

            if (!error.type().isSilent()) {
                blade.logger().error(
                    "Failed to parse %s's command input for command `%s`: %s",
                    sender.toString(),
                    label, TokenizerError.generateFancyMessage(error));
            }
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while %s was tab completing the command `%s`.",
                sender.toString(), label);
        }

        return Collections.emptyList();
    }

    private void sendHelpMessage(@NotNull CommandSource sender,
                                 @NotNull Context context,
                                 @NotNull List<ResolvedCommand> nodes) {
        List<BladeCommand> allCommands = new ArrayList<>();

        nodes.forEach(node ->
            node.collectCommandsInto(allCommands));

        if (allCommands.isEmpty()) {
            sender.sendMessage(UNKNOWN_COMMAND_MESSAGE);
            return;
        }

        List<Component> lines = blade.<Component>configuration().helpGenerator().generate(context, allCommands);

        lines.forEach(sender::sendMessage);
    }
}