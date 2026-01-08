package me.vaperion.blade.minestom.container;

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
import me.vaperion.blade.minestom.context.MinestomSender;
import me.vaperion.blade.tokenizer.TokenizerError;
import me.vaperion.blade.tokenizer.input.CommandInput;
import me.vaperion.blade.tokenizer.input.InputOption;
import me.vaperion.blade.tree.CommandTreeNode;
import me.vaperion.blade.util.ErrorMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static me.vaperion.blade.util.BladeHelper.*;
import static net.kyori.adventure.text.Component.text;

@Getter
public final class MinestomContainer extends Command implements Container {

    public static final ContainerCreator<MinestomContainer> CREATOR = MinestomContainer::new;

    private static final Component UNKNOWN_COMMAND_MESSAGE = text(
        "Unknown command. Type \"/help\" for help."
    );

    private final Blade blade;
    private final String label;

    private MinestomContainer(@NotNull Blade blade, @NotNull String label) {
        super(label);

        this.blade = blade;
        this.label = label;

        // Minestom uses a custom builder instead of using Brigadier, even though they end up converting it internally...
        // Instead of that we just use a greedy string and handle it inside Blade.
        addSyntax(
            this::execute,
            ArgumentType.StringArray("args")
                .setDefaultValue(new String[0])
                .setSuggestionCallback(this::suggest)
        );

        MinecraftServer.getCommandManager().register(this);
    }

    @Override
    public void unregister() {
        MinecraftServer.getCommandManager().unregister(this);
    }

    private void execute(@NotNull CommandSender sender,
                         @NotNull CommandContext minestomContext) {
        String commandLine = removeCommandQualifier(minestomContext.getInput());

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

        String label = node.matchedLabelOr(
            commandLine.split(" ")[0]
        );

        String[] args = removePrefix(
            commandLine,
            label
        ).split(" ");

        Context context = new Context(
            blade,
            new MinestomSender(sender),
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

        BladeCommand command = node.command();

        if (!Objects.requireNonNull(command).hasPermission(context)) {
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

                    if (!input.mergeTokensToFormWholeLabel(Objects.requireNonNull(node.matchedLabel()))) {
                        // Failed to merge label - can't execute command.
                        throw new BladeFatalError("Failed to parse command input for execution.");
                    }

                    context.updateArgumentsFromInput(input);

                    ErrorMessage error = blade.executor().execute(context, input, node);

                    if (error != null) {
                        switch (error.type()) {
                            case LINES:
                                for (String line : error.lines()) {
                                    sender.sendMessage(text(line, NamedTextColor.RED));
                                }
                                break;

                            case SHOW_COMMAND_USAGE:
                                command.usageMessage().sendTo(context);
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
                        text(e.getMessage(), NamedTextColor.RED)
                    );
                } catch (BladeInvocationError e) {
                    sender.sendMessage(
                        text(ERROR_MESSAGE, NamedTextColor.RED)
                    );

                    blade.logger().error(e, "Blade failed to invoke the method for command `%s` executed by %s. This is most likely a bug in your plugin.",
                        label, sender.toString());
                } catch (BladeImplementationError e) {
                    sender.sendMessage(
                        text(ERROR_MESSAGE, NamedTextColor.RED)
                    );
                    command.usageMessage().sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in your plugin.",
                        sender.toString(), label);
                } catch (BladeInternalError e) {
                    sender.sendMessage(
                        text(ERROR_MESSAGE, NamedTextColor.RED)
                    );
                    command.usageMessage().sendTo(context);

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

    private void sendHelpMessage(@NotNull CommandSender sender,
                                 @NotNull Context context,
                                 @NotNull List<ResolvedCommand> nodes,
                                 boolean sendUnknownCommandMessage) {
        List<BladeCommand> allCommands = new ArrayList<>();

        nodes.forEach(node ->
            node.collectCommandsInto(allCommands));

        if (allCommands.isEmpty() && sendUnknownCommandMessage) {
            sender.sendMessage(UNKNOWN_COMMAND_MESSAGE);
            return;
        }

        List<Component> lines = blade.<Component>configuration().helpGenerator().generate(context, allCommands);

        lines.forEach(sender::sendMessage);
    }

    private void suggest(@NotNull CommandSender sender,
                         @NotNull CommandContext minestomContext,
                         @NotNull Suggestion suggestion) {
        for (String s : doSuggest(sender, minestomContext)) {
            suggestion.addEntry(new SuggestionEntry(s));
        }
    }

    @NotNull
    private List<String> doSuggest(@NotNull CommandSender sender,
                                   @NotNull CommandContext minestomContext) {
        if (!blade.configuration().tabCompleter().isDefault())
            return Collections.emptyList();

        String commandLine = removeCommandQualifier(minestomContext.getInput());

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

                String[] args = removePrefix(
                    removeCommandQualifier(commandLine),
                    node.matchedLabelOr("")
                ).split(" ");

                Context context = new Context(
                    blade,
                    new MinestomSender(sender),
                    node.matchedLabel(),
                    args
                );

                CommandInput input = Objects.requireNonNull(node.command()).tokenize(
                    context.sender(),
                    "/" + removeCommandQualifier(commandLine)
                );

                if (!input.mergeTokensToFormWholeLabel(Objects.requireNonNull(node.matchedLabel()))) {
                    // Failed to merge label - can't suggest arguments.
                    throw new BladeFatalError("Failed to parse command input for tab completion.");
                }

                context.updateArgumentsFromInput(input);

                return blade.suggestionProvider().suggest(
                    context,
                    input,
                    SuggestionType.ARGUMENTS
                );
            }

            // Only found command stub - suggest subcommands.

            String[] args = removeCommandQualifier(minestomContext.getInput()).split(" ");

            Context context = new Context(
                blade,
                new MinestomSender(sender),
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
                text(ERROR_MESSAGE, NamedTextColor.RED)
            );

            blade.logger().error(e, "An error occurred while %s was tab completing the command `%s`. This is a bug in your plugin.",
                sender.toString(), label);
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
}
