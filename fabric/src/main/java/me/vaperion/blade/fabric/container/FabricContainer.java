package me.vaperion.blade.fabric.container;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
import me.vaperion.blade.fabric.context.FabricSender;
import me.vaperion.blade.impl.node.ResolvedCommand;
import me.vaperion.blade.impl.suggestions.SuggestionType;
import me.vaperion.blade.tokenizer.TokenizerError;
import me.vaperion.blade.tokenizer.input.CommandInput;
import me.vaperion.blade.tokenizer.input.InputOption;
import me.vaperion.blade.tree.CommandTreeNode;
import me.vaperion.blade.util.ErrorMessage;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static me.vaperion.blade.util.BladeHelper.*;

@Getter
public final class FabricContainer implements Container {

    public static final ContainerCreator<FabricContainer> CREATOR = FabricContainer::new;

    private static final Text UNKNOWN_COMMAND_MESSAGE = Text.literal(
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

    public boolean execute(@NotNull CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource sender = ctx.getSource();

        String commandLine = removeCommandQualifier(ctx.getInput());

        ResolvedCommand node = blade.nodeResolver().resolve(
            commandLine
        );

        if (node == null) {
            sender.sendMessage(UNKNOWN_COMMAND_MESSAGE);

            if (blade.configuration().verbose())
                blade.logger().info(
                    "%s tried to execute unknown command: `%s`. This is most likely a bug in Blade, not your plugin. Please report it.",
                    sender.getName(),
                    commandLine
                );
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

        BladeCommand command = node.command();

        if (!Objects.requireNonNull(command).hasPermission(context)) {
            sender.sendMessage(
                Text.literal(command.permissionMessage()).formatted(Formatting.RED)
            );
            return true;
        }

        try {
            Runnable runnable = () -> {
                try {
                    CommandInput input = command.tokenize(
                        context.sender(),
                        "/" + commandLine
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
                                    sender.sendMessage(
                                        Text.literal(line).formatted(Formatting.RED)
                                    );
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
                        Text.literal(e.getMessage()).formatted(Formatting.RED)
                    );
                } catch (BladeInvocationError e) {
                    sender.sendMessage(
                        Text.literal(ERROR_MESSAGE).formatted(Formatting.RED)
                    );

                    blade.logger().error(e, "Blade failed to invoke the method for command `%s` executed by %s. This is most likely a bug in your plugin.",
                        label, sender.getName());
                } catch (BladeImplementationError e) {
                    sender.sendMessage(
                        Text.literal(ERROR_MESSAGE).formatted(Formatting.RED)
                    );
                    command.usageMessage().sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in your plugin.",
                        sender.getName(), label);
                } catch (BladeInternalError e) {
                    sender.sendMessage(
                        Text.literal(ERROR_MESSAGE).formatted(Formatting.RED)
                    );
                    command.usageMessage().sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                        sender.getName(), label);
                } catch (TokenizerError error) {
                    sender.sendMessage(Text.literal(error.formatForChat())
                        .formatted(Formatting.RED));
                    command.usageMessage().sendTo(context);

                    if (!error.type().isSilent()) {
                        blade.logger().error(
                            "Failed to parse %s's command input for command `%s`: %s",
                            sender.getName(),
                            label, TokenizerError.generateFancyMessage(error));
                    }
                } catch (Throwable t) {
                    blade.logger().error(t, "An unexpected error occurred while %s was executing the command `%s`.",
                        sender.getName(), label);
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

            return true;
        } catch (Throwable t) {
            blade.logger().error(t, "An unexpected error occurred while %s was executing the command `%s`.",
                sender.getName(), label);
        }

        return false;
    }

    public void suggest(@NotNull CommandContext<ServerCommandSource> ctx,
                        @NotNull SuggestionsBuilder builder) {
        var suggestions = doSuggest(ctx);

        for (var suggestion : suggestions) {
            builder.suggest(suggestion);
        }
    }

    @NotNull
    private List<String> doSuggest(@NotNull CommandContext<ServerCommandSource> ctx) {
        if (!blade.configuration().tabCompleter().isDefault())
            return Collections.emptyList();

        var sender = ctx.getSource();

        ResolvedCommand node = blade.nodeResolver().resolve(
            ctx.getInput()
        );

        if (node == null) {
            // No main command and not a stub either - not a blade command at all?
            return Collections.emptyList();
        }

        try {
            if (!node.isStub()) {
                // Found exact command, we can suggest arguments here.

                String[] args = removePrefix(
                    removeCommandQualifier(ctx.getInput()),
                    node.matchedLabelOr("")
                ).split(" ");

                Context context = new Context(
                    blade,
                    new FabricSender(blade, sender),
                    node.matchedLabel(),
                    args
                );

                CommandInput input = Objects.requireNonNull(node.command()).tokenize(
                    context.sender(),
                    removeCommandQualifier(ctx.getInput())
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

            String[] args = removeCommandQualifier(ctx.getInput()).split(" ");

            Context context = new Context(
                blade,
                new FabricSender(blade, sender),
                "",
                args
            );

            CommandInput input = new CommandInput(
                blade,
                null,
                removeCommandQualifier(ctx.getInput()),
                InputOption.DISALLOW_FLAGS
            );

            input.tokenize();

            return blade.suggestionProvider().suggest(
                context,
                input,
                SuggestionType.SUBCOMMANDS
            );
        } catch (BladeImplementationError e) {
            sender.sendMessage(Text.literal(ERROR_MESSAGE).formatted(Formatting.RED));

            blade.logger().error(e, "An error occurred while %s was tab completing the command `%s`. This is a bug in your plugin.",
                sender.getName(), label);
        } catch (BladeInternalError e) {
            sender.sendMessage(Text.literal(ERROR_MESSAGE).formatted(Formatting.RED));

            blade.logger().error(e, "An error occurred while %s was tab completing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                sender.getName(), label);
        } catch (BladeFatalError ex) {
            sender.sendMessage(Text.literal(ex.getMessage()).formatted(Formatting.RED));
        } catch (TokenizerError error) {
            // Don't send tokenizer errors to the user during tab completion - just log them.

            if (!error.type().isSilent()) {
                blade.logger().error(
                    "Failed to parse %s's command input for command `%s`: %s",
                    sender.getName(),
                    label, TokenizerError.generateFancyMessage(error));
            }
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while %s was tab completing the command `%s`.",
                sender.getName(), label);
        }

        return Collections.emptyList();
    }

    private void sendHelpMessage(@NotNull ServerCommandSource sender,
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

        var lines = blade.<Text>configuration().helpGenerator().generate(context, allCommands);

        lines.forEach(sender::sendMessage);
    }

}
