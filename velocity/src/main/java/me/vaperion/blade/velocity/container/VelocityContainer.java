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
import me.vaperion.blade.exception.internal.BladeImplementationError;
import me.vaperion.blade.exception.internal.BladeInternalError;
import me.vaperion.blade.exception.internal.BladeInvocationError;
import me.vaperion.blade.impl.node.ResolvedCommand;
import me.vaperion.blade.impl.suggestions.SuggestionType;
import me.vaperion.blade.platform.api.BladeMessages;
import me.vaperion.blade.tokenizer.TokenizerError;
import me.vaperion.blade.tokenizer.input.CommandInput;
import me.vaperion.blade.tokenizer.input.InputOption;
import me.vaperion.blade.tree.CommandTreeNode;
import me.vaperion.blade.util.ErrorMessage;
import me.vaperion.blade.util.command.CommandExecutionWrapper;
import me.vaperion.blade.velocity.BladeVelocityPlatform;
import me.vaperion.blade.velocity.context.VelocitySender;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static me.vaperion.blade.util.BladeHelper.*;

@Getter
public class VelocityContainer implements RawCommand, Container {

    public static final ContainerCreator<VelocityContainer> CREATOR = VelocityContainer::new;

    private final Blade blade;
    private final String label;

    private VelocityContainer(@NotNull Blade blade, @NotNull String label) {
        this.blade = blade;
        this.label = label;

        ProxyServer proxyServer = blade.platformAs(BladeVelocityPlatform.class).server();
        CommandManager commandManager = proxyServer.getCommandManager();

        CommandMeta meta = commandManager.metaBuilder(label).build();
        commandManager.register(meta, this);
    }

    @Override
    public void unregister() {
        ProxyServer proxyServer = blade.platformAs(BladeVelocityPlatform.class).server();
        CommandManager commandManager = proxyServer.getCommandManager();

        commandManager.unregister(this.label);
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

        BladeMessages messages = blade.configuration().messages();

        if (node == null) {
            sender.sendMessage(messages.unknownCommand());

            if (blade.configuration().verbose()) {
                blade.logger().info(
                    "%s tried to execute unknown command: `%s`. This is most likely a bug in Blade, not your plugin. Please report it.",
                    sender.toString(),
                    commandLine
                );
            }

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
                node.subcommands(),
                true);
            return;
        }

        BladeCommand command = Objects.requireNonNull(node.command());

        if (!node.hasPermission(context)) {
            sender.sendMessage(messages.permissionMessage(command));
            return;
        }

        try {
            Runnable runnable = () -> {
                try {
                    ErrorMessage error = blade.executor().execute(
                        context,
                        node,
                        "/" + removeCommandQualifier(commandLine)
                    );

                    if (error != null) {
                        switch (error.type()) {
                            case LINES:
                                for (String line : error.lines()) {
                                    sender.sendMessage(messages.error(line));
                                }
                                break;

                            case SHOW_COMMAND_USAGE:
                                if (error.command() != null) {
                                    error.command().usageMessage(context).sendTo(context);
                                    break;
                                }

                                for (BladeCommand overload : node.overloads()) {
                                    // Don't reveal overloads the sender cannot use.
                                    if (!overload.hasPermission(context)) continue;

                                    overload.usageMessage(context).sendTo(context);
                                }
                                break;

                            case SHOW_COMMAND_HELP:
                                CommandTreeNode parent = node.treeNode().parent();

                                List<ResolvedCommand> subcommands = node.subcommands();

                                if (parent != null) {
                                    ResolvedCommand parentCommand = blade.nodeResolver().resolve(parent.label());

                                    if (parentCommand != null) {
                                        subcommands = parentCommand.subcommands();
                                    }
                                }

                                sendHelpMessage(sender, context, subcommands, false);
                                break;
                        }
                    }
                } catch (BladeParseError | BladeFatalError e) {
                    sender.sendMessage(
                        messages.error(e.getMessage())
                    );
                } catch (BladeInvocationError e) {
                    sender.sendMessage(
                        messages.genericError()
                    );

                    blade.logger().error(e, "Blade failed to invoke the method for command `%s` executed by %s. This is most likely a bug in your plugin.",
                        label, sender.toString());
                } catch (BladeImplementationError e) {
                    sender.sendMessage(
                        messages.genericError()
                    );
                    command.usageMessage(context).sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in your plugin.",
                        sender.toString(), label);
                } catch (BladeInternalError e) {
                    sender.sendMessage(
                        messages.genericError()
                    );
                    command.usageMessage(context).sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                        sender.toString(), label);
                } catch (TokenizerError error) {
                    sender.sendMessage(
                        messages.error(error.formatForChat())
                    );

                    if (error.type().shouldLog()) {
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
                CommandExecutionWrapper.runAsync(blade, command, runnable);
            } else {
                CommandExecutionWrapper.runSync(blade, command, runnable);
            }
        } catch (Throwable t) {
            blade.logger().error(t, "An unexpected error occurred while %s was executing the command `%s`.",
                sender.toString(), label);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = splitSuggestionArguments(invocation.arguments());
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

                return blade.suggestionProvider().suggestNode(
                    context,
                    node,
                    "/" + removeCommandQualifier(commandLine),
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
        } catch (BladeImplementationError e) {
            sender.sendMessage(
                blade.configuration().messages().genericError()
            );

            blade.logger().error(e, "An error occurred while %s was tab completing the command `%s`. This is a bug in your plugin.",
                sender.toString(), label);
        } catch (BladeInternalError e) {
            sender.sendMessage(
                blade.configuration().messages().genericError()
            );

            blade.logger().error(e, "An error occurred while %s was tab completing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                sender.toString(), label);
        } catch (BladeFatalError ex) {
            sender.sendMessage(
                blade.configuration().messages().error(ex.getMessage())
            );
        } catch (TokenizerError ignored) {
            // Incomplete input is normal during tab completion.
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while %s was tab completing the command `%s`.",
                sender.toString(), label);
        }

        return Collections.emptyList();
    }

    private void sendHelpMessage(@NotNull CommandSource sender,
                                 @NotNull Context context,
                                 @NotNull List<ResolvedCommand> nodes,
                                 boolean sendUnknownCommandMessage) {
        List<BladeCommand> allCommands = new ArrayList<>();

        nodes.forEach(node ->
            node.collectCommandsInto(allCommands));

        if (allCommands.isEmpty() && sendUnknownCommandMessage) {
            sender.sendMessage(blade.configuration().messages().unknownCommand());
            return;
        }

        List<Component> lines = blade.configuration().helpGenerator().generate(context, allCommands);

        lines.forEach(sender::sendMessage);
    }
}
