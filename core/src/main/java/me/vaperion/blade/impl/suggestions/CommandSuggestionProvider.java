package me.vaperion.blade.impl.suggestions;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.exception.internal.BladeInternalError;
import me.vaperion.blade.tokenizer.input.CommandInput;
import me.vaperion.blade.tokenizer.input.token.impl.LabelToken;
import me.vaperion.blade.tree.CommandTree;
import me.vaperion.blade.tree.CommandTreeNode;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static me.vaperion.blade.util.BladeHelper.arrayToEnumSet;

@RequiredArgsConstructor
public final class CommandSuggestionProvider {

    private final Blade blade;

    /**
     * Generates suggestions for command input.
     *
     * @param context the command context
     * @param input   the command input
     * @param types   the suggestion types
     * @return a list of suggestions
     */
    @NotNull
    public List<String> suggest(@NotNull Context context,
                                @NotNull CommandInput input,
                                @NotNull SuggestionType... types) {
        return suggest(context, input, arrayToEnumSet(SuggestionType.class, types));
    }

    /**
     * Generates suggestions for command input.
     *
     * @param context the command context
     * @param input   the command input
     * @param types   the suggestion types
     * @return a list of suggestions
     */
    @NotNull
    public List<String> suggest(@NotNull Context context,
                                @NotNull CommandInput input,
                                @NotNull EnumSet<SuggestionType> types) {
        input.ensureTokenized();

        if (input.bladeCommand() != null && !input.bladeCommand().hasPermission(context)) {
            // No suggestions if the user doesn't have permission for the command
            return Collections.emptyList();
        }

        SuggestionsBuilder builder = new SuggestionsBuilder();

        String label = input.label()
            .map(LabelToken::name)
            .orElse("");

        if (types.contains(SuggestionType.SUBCOMMANDS)) {
            suggestSubcommands(context, input, label, builder);
        }

        if (input.bladeCommand() == null) {
            // We can't possibly suggest arguments if there's no command
            return builder.build();
        }

        if (input.bladeCommand().usesBladeContext()) {
            // We can't suggest arguments for context-based commands
            return Collections.emptyList();
        }

        if (types.contains(SuggestionType.ARGUMENTS)) {
            suggestCommandArguments(context, input, label, builder);
        }

        return builder.build();
    }

    private void suggestSubcommands(@NotNull Context context,
                                    @NotNull CommandInput input,
                                    @NotNull String baseCommand,
                                    @NotNull SuggestionsBuilder builder) {
        CommandTree tree = blade.commandTree();
        CommandTreeNode rootNode = tree.root(baseCommand);

        if (rootNode == null) return;

        String[] inputParts = input.unslashedInput().split(" ");
        CommandTreeNode currentNode = rootNode;

        for (int i = 1; i < inputParts.length - (input.endsInWhitespace() ? 0 : 1); i++) {
            CommandTreeNode child = currentNode.child(inputParts[i]);
            if (child == null) return; // Invalid path
            currentNode = child;
        }

        String partialWord = input.endsInWhitespace() ? "" : inputParts[inputParts.length - 1];

        for (CommandTreeNode child : currentNode.children().values()) {
            if (!hasAccessibleCommand(child, context)) continue;

            String childLabel = child.label();

            if (childLabel.toLowerCase().startsWith(partialWord.toLowerCase())) {
                builder.suggest(childLabel);
            }
        }
    }

    private boolean hasAccessibleCommand(@NotNull CommandTreeNode node,
                                         @NotNull Context context) {
        if (node.isLeaf()) {
            BladeCommand cmd = node.command();
            if (!cmd.hidden() && cmd.hasPermission(context)) {
                return true;
            }
        }

        for (CommandTreeNode child : node.children().values()) {
            if (hasAccessibleCommand(child, context)) {
                return true;
            }
        }

        return false;
    }

    private void suggestCommandArguments(@NotNull Context context,
                                         @NotNull CommandInput input,
                                         @NotNull String baseCommand,
                                         @NotNull SuggestionsBuilder builder) {
        BladeCommand command = input.bladeCommand();
        if (command == null) return;

        DefinedArgument argument;
        ArgumentProvider<?> provider;
        InputArgument inputArgument;

        if (input.endsInWhitespace()) {
            // Complete next argument

            int index = input.arguments().size();

            if (command.arguments().size() <= index) {
                // No more arguments to complete
                return;
            }

            argument = command.arguments().get(index);

            provider = argument.hasCustomCompleter()
                ? argument.customCompleter()
                : command.argumentProviders().get(index);

            inputArgument = new InputArgument(
                argument,
                "",
                InputArgument.Status.NOT_PRESENT
            );

            inputArgument.data().addAll(argument.data());
            inputArgument.addAnnotations(argument.annotations());

            // All suggestions are valid for a new argument
            builder.filter(null);
        } else {
            // Complete current argument

            int index = input.arguments().size() - 1;

            if (index < 0 || command.arguments().size() <= index) {
                // No argument to complete
                return;
            }

            String token = input.arguments()
                .get(index)
                .value();

            argument = command.arguments().get(index);

            provider = argument.hasCustomCompleter()
                ? argument.customCompleter()
                : command.argumentProviders().get(index);

            inputArgument = new InputArgument(
                argument,
                token,
                token.isEmpty()
                    ? InputArgument.Status.NOT_PRESENT
                    : InputArgument.Status.PRESENT
            );

            inputArgument.data().addAll(argument.data());
            inputArgument.addAnnotations(argument.annotations());

            // Only allow suggestions that start with the current token
            builder.filter(s -> s.startsWith(token));
        }

        if (provider == null) {
            throw new BladeInternalError(String.format(
                "No argument provider found for argument `%s` of command `%s`",
                argument.name(),
                baseCommand
            ));
        }

        try {
            provider.suggest(context, inputArgument, builder);
        } catch (BladeParseError error) {
            if (argument.isOptional() &&
                Objects.requireNonNull(argument.optional()).treatErrorAsEmpty() &&
                error.isRecoverable()) {
                return;
            }

            throw error;
        } finally {
            builder.filter(null);
        }
    }

}