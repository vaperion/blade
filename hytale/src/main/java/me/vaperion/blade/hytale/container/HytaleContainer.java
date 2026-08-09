package me.vaperion.blade.hytale.container;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.*;
import com.hypixel.hytale.server.core.entity.entities.Player;
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
import me.vaperion.blade.hytale.context.HytaleSender;
import me.vaperion.blade.impl.node.ResolvedCommand;
import me.vaperion.blade.tokenizer.TokenizerError;
import me.vaperion.blade.tree.CommandTreeNode;
import me.vaperion.blade.util.ErrorMessage;
import me.vaperion.blade.util.command.CommandExecutionWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.Message.raw;
import static me.vaperion.blade.util.BladeHelper.removeCommandQualifier;
import static me.vaperion.blade.util.BladeHelper.removePrefix;

@Getter
public final class HytaleContainer extends AbstractCommand implements Container {

    public static final ContainerCreator<HytaleContainer> CREATOR = HytaleContainer::new;

    private static final CompletableFuture<Void> VOID_FUTURE = CompletableFuture.completedFuture(null);

    private final Blade blade;
    private final String label;

    public HytaleContainer(@NotNull Blade blade, @NotNull String label) {
        super(label, ""); // no description

        this.blade = blade;
        this.label = label;

        setAllowsExtraArguments(true);

        HytaleServer.get().getCommandManager().register(this);
    }

    @Override
    public void unregister() {
        // No-op: Hytale does not support unregistering commands currently
    }

    // We have to override acceptCall as Hytale's parser interferes with Blade's parsing
    @Override
    public @NotNull CompletableFuture<Void> acceptCall(@NotNull CommandSender sender,
                                                       @NotNull ParserContext parserContext,
                                                       @NotNull ParseResult parseResult) {
        String commandLine = removeCommandQualifier(parserContext.getInputString());

        ResolvedCommand node = blade.nodeResolver().resolve(
            commandLine
        );

        if (node == null) {
            sender.sendMessage(
                raw("Command not found! " + commandLine)
            );

            if (blade.configuration().verbose()) {
                blade.logger().info(
                    "%s tried to execute unknown command: `%s`. This is most likely a bug in Blade, not your plugin. Please report it.",
                    sender.toString(),
                    commandLine
                );
            }

            return VOID_FUTURE;
        }

        String label = node.matchedLabelOr(
            commandLine.split(" ")[0]
        );

        String[] args = removePrefix(
            commandLine,
            label
        ).split(" ");

        Context context = new Context(
            blade,
            new HytaleSender(sender),
            node.matchedLabelOr(label),
            args
        );

        if (node.isStub() || node.command() == null) {
            sendHelpMessage(sender,
                context,
                node.subcommands(),
                true);

            return VOID_FUTURE;
        }

        BladeCommand command = Objects.requireNonNull(node.command());

        if (!node.hasPermission(context)) {
            context.sender().sendMessage(blade.configuration().messages().permissionMessage(command));

            return VOID_FUTURE;
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
                                    context.sender().sendMessage(blade.configuration().messages().error(line));
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
                    context.sender().sendMessage(blade.configuration().messages().error(e.getMessage()));
                } catch (BladeInvocationError e) {
                    context.sender().sendMessage(blade.configuration().messages().genericError());

                    blade.logger().error(e, "Blade failed to invoke the method for command `%s` executed by %s. This is most likely a bug in your plugin.",
                        label, sender.toString());
                } catch (BladeImplementationError e) {
                    context.sender().sendMessage(blade.configuration().messages().genericError());
                    command.usageMessage(context).sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in your plugin.",
                        sender.toString(), label);
                } catch (BladeInternalError e) {
                    context.sender().sendMessage(blade.configuration().messages().genericError());
                    command.usageMessage(context).sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                        sender.toString(), label);
                } catch (TokenizerError error) {
                    context.sender().sendMessage(blade.configuration().messages().error(error.formatForChat()));

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
                // Hytale ECS has thread checks, and users probably want to access
                // data related to the player or their world, so it makes sense for us to run
                // the command on the world's thread instead of the current thread.
                if (sender instanceof Player player && player.getWorld() != null) {
                    player.getWorld().execute(runnable);
                } else {
                    // Fallback to running on the main server thread
                    CommandExecutionWrapper.runSync(blade, command, runnable);
                }
            }
        } catch (Throwable t) {
            blade.logger().error(t, "An unexpected error occurred while %s was executing the command `%s`.",
                sender.toString(), label);
        }

        return VOID_FUTURE;
    }

    @Override
    protected @Nullable CompletableFuture<Void> execute(@NotNull CommandContext commandContext) {
        // This method will not be called as we override acceptCall
        throw new UnsupportedOperationException();
    }

    private void sendHelpMessage(@NotNull CommandSender sender,
                                 @NotNull Context context,
                                 @NotNull List<ResolvedCommand> nodes,
                                 boolean sendUnknownCommandMessage) {
        List<BladeCommand> allCommands = new ArrayList<>();

        nodes.forEach(node ->
            node.collectCommandsInto(allCommands));

        if (allCommands.isEmpty() && sendUnknownCommandMessage) {
            sender.sendMessage(
                raw("Command not found!")
            );
            return;
        }

        List<net.kyori.adventure.text.Component> lines = blade.configuration().helpGenerator().generate(context, allCommands);

        lines.forEach(context.sender()::sendMessage);
    }
}
