package me.vaperion.blade.fabric.container;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.brigadier.BrigadierRichSuggestionsBuilder;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.Container;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.exception.internal.BladeFatalError;
import me.vaperion.blade.exception.internal.BladeImplementationError;
import me.vaperion.blade.exception.internal.BladeInternalError;
import me.vaperion.blade.exception.internal.BladeInvocationError;
import me.vaperion.blade.fabric.context.FabricSender;
import me.vaperion.blade.impl.node.ResolvedCommand;
import me.vaperion.blade.impl.suggestions.SuggestionType;
import me.vaperion.blade.tokenizer.TokenizerError;
import me.vaperion.blade.tokenizer.input.CommandInput;
import me.vaperion.blade.tokenizer.input.InputOption;
import me.vaperion.blade.tree.CommandTreeNode;
import me.vaperion.blade.util.ErrorMessage;
import me.vaperion.blade.util.command.CommandExecutionWrapper;
import me.vaperion.blade.util.command.RichSuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static me.vaperion.blade.util.BladeHelper.*;

@Getter
public final class FabricContainer implements Container {

    public static final ContainerCreator<FabricContainer> CREATOR = FabricContainer::new;

    private static final Component UNKNOWN_COMMAND_MESSAGE = Component.literal(
        "Unknown command. Type \"/help\" for help."
    );

    private final Blade blade;
    private final String label;

    private FabricContainer(@NotNull Blade blade,
                            @NotNull String label) {
        this.blade = blade;
        this.label = label;
    }

    @Override
    public void unregister() {
        // No-op: Command registration is managed through Brigadier directly.
    }

    public boolean execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack sender = ctx.getSource();

        String commandLine = removeCommandQualifier(ctx.getInput());

        ResolvedCommand node = blade.nodeResolver().resolve(
            commandLine
        );

        if (node == null) {
            sender.sendSystemMessage(UNKNOWN_COMMAND_MESSAGE);

            if (blade.configuration().verbose()) {
                blade.logger().info(
                    "%s tried to execute unknown command: `%s`. This is most likely a bug in Blade, not your plugin. Please report it.",
                    sender.getTextName(),
                    commandLine
                );
            }

            return false;
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
            new FabricSender(blade, sender),
            label,
            args
        );

        if (node.isStub() || node.command() == null) {
            sendHelpMessage(sender,
                context,
                node.subcommands(),
                true);
            return true;
        }

        BladeCommand command = Objects.requireNonNull(node.command());

        if (!node.hasPermission(context)) {
            context.sender().sendMessage(blade.configuration().messages().permissionMessage(command));
            return true;
        }

        try {
            Runnable runnable = () -> {
                try {
                    ErrorMessage error = blade.executor().execute(
                        context,
                        node,
                        "/" + commandLine
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
                        label, sender.getTextName());
                } catch (BladeImplementationError e) {
                    context.sender().sendMessage(blade.configuration().messages().genericError());
                    command.usageMessage(context).sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in your plugin.",
                        sender.getTextName(), label);
                } catch (BladeInternalError e) {
                    context.sender().sendMessage(blade.configuration().messages().genericError());
                    command.usageMessage(context).sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                        sender.getTextName(), label);
                } catch (TokenizerError error) {
                    context.sender().sendMessage(blade.configuration().messages().error(error.formatForChat()));
                    command.usageMessage(context).sendTo(context);

                    if (!error.type().isSilent()) {
                        blade.logger().error(
                            "Failed to parse %s's command input for command `%s`: %s",
                            sender.getTextName(),
                            label, TokenizerError.generateFancyMessage(error));
                    }
                } catch (Throwable t) {
                    blade.logger().error(t, "An unexpected error occurred while %s was executing the command `%s`.",
                        sender.getTextName(), label);
                }
            };

            if (command.async()) {
                CommandExecutionWrapper.runAsync(blade, command, runnable);
            } else {
                CommandExecutionWrapper.runSync(blade, command, runnable);
            }

            return true;
        } catch (Throwable t) {
            blade.logger().error(t, "An unexpected error occurred while %s was executing the command `%s`.",
                sender.getTextName(), label);
        }

        return false;
    }

    public void suggest(@NotNull CommandContext<CommandSourceStack> ctx,
                        @NotNull SuggestionsBuilder builder) {
        suggest(ctx, new BrigadierRichSuggestionsBuilder(builder));
    }

    public void suggest(@NotNull CommandContext<CommandSourceStack> ctx,
                        @NotNull RichSuggestionsBuilder suggestions) {
        if (!blade.configuration().tabCompleter().isDefault())
            return;

        var sender = ctx.getSource();
        String commandLine = removeCommandQualifier(suggestions.input());

        ResolvedCommand node = blade.nodeResolver().resolve(
            commandLine
        );

        if (node == null) {
            // No main command and not a stub either - not a blade command at all?
            return;
        }

        try {
            if (!node.isStub()) {
                // Found exact command, we can suggest arguments here.

                String[] args = splitSuggestionArguments(removePrefix(
                    commandLine,
                    node.matchedLabelOr("")
                ));

                Context context = new Context(
                    blade,
                    new FabricSender(blade, sender),
                    node.matchedLabel(),
                    args
                );

                blade.suggestionProvider().suggestNode(
                    context,
                    node,
                    commandLine,
                    EnumSet.of(SuggestionType.ARGUMENTS),
                    suggestions
                );
                return;
            }

            // Only found command stub - suggest subcommands.

            String[] args = splitSuggestionArguments(commandLine);

            Context context = new Context(
                blade,
                new FabricSender(blade, sender),
                "",
                args
            );

            CommandInput input = new CommandInput(
                blade,
                null,
                commandLine,
                InputOption.DISALLOW_FLAGS
            );

            input.tokenize();

            blade.suggestionProvider().suggest(
                context,
                input,
                EnumSet.of(SuggestionType.SUBCOMMANDS),
                suggestions
            );
        } catch (BladeImplementationError e) {
            new FabricSender(blade, sender).sendMessage(blade.configuration().messages().genericError());

            blade.logger().error(e, "An error occurred while %s was tab completing the command `%s`. This is a bug in your plugin.",
                sender.getTextName(), label);
        } catch (BladeInternalError e) {
            new FabricSender(blade, sender).sendMessage(blade.configuration().messages().genericError());

            blade.logger().error(e, "An error occurred while %s was tab completing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                sender.getTextName(), label);
        } catch (BladeFatalError ex) {
            new FabricSender(blade, sender).sendMessage(blade.configuration().messages().error(ex.getMessage()));
        } catch (TokenizerError error) {
            // Don't send tokenizer errors to the user during tab completion - just log them.

            if (!error.type().isSilent()) {
                blade.logger().error(
                    "Failed to parse %s's command input for command `%s`: %s",
                    sender.getTextName(),
                    label, TokenizerError.generateFancyMessage(error));
            }
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while %s was tab completing the command `%s`.",
                sender.getTextName(), label);
        }
    }

    private void sendHelpMessage(@NotNull CommandSourceStack sender,
                                 @NotNull Context context,
                                 @NotNull List<ResolvedCommand> nodes,
                                 boolean sendUnknownCommandMessage) {
        List<BladeCommand> allCommands = new ArrayList<>();

        nodes.forEach(node ->
            node.collectCommandsInto(allCommands));

        if (allCommands.isEmpty() && sendUnknownCommandMessage) {
            sender.sendSystemMessage(UNKNOWN_COMMAND_MESSAGE);
            return;
        }

        var lines = blade.configuration().helpGenerator().generate(context, allCommands);

        lines.forEach(context.sender()::sendMessage);
    }

}
