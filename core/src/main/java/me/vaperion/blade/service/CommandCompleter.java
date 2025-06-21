package me.vaperion.blade.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.argument.Argument;
import me.vaperion.blade.argument.Argument.Type;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.command.Command;
import me.vaperion.blade.command.Parameter;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.context.WrappedSender;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.exception.StacklessErrorMessage;
import me.vaperion.blade.util.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class CommandCompleter {

    private final Blade blade;

    @Nullable
    public List<String> suggest(@NotNull String commandLine,
                                @NotNull Supplier<WrappedSender<?>> senderSupplier) {
        List<String> suggestions = new ArrayList<>();
        suggestSubCommand(suggestions, commandLine, senderSupplier);

        String[] commandParts = commandLine.split(" ");
        Tuple<Command, String> resolved = blade.getResolver().resolveCommand(commandParts);

        if (resolved == null) return suggestions.isEmpty() ? null : suggestions;
        if (resolved.getLeft().isContextBased())
            return suggestions.isEmpty() ? null : suggestions;

        Command command = resolved.getLeft();
        String foundAlias = resolved.getRight();

        List<String> argList = new ArrayList<>(Arrays.asList(commandParts));
        argList.subList(0, Math.max(1, foundAlias.split(" ").length)).clear();

        if (commandLine.endsWith(" ")) argList.add("");
        String[] actualArguments = argList.toArray(new String[0]);

        Context context = new Context(blade, senderSupplier.get(), foundAlias, actualArguments);
        if (!blade.getPermissionTester().testPermission(context, command))
            return suggestions.isEmpty() ? null : suggestions;

        suggest(suggestions, context, command, actualArguments);
        return suggestions;
    }

    public void suggest(@NotNull List<String> suggestions, @NotNull Context context,
                        @NotNull Command command, @NotNull String[] args) throws BladeExitMessage {
        if (command.isContextBased()) return;

        try {
            List<String> argumentList = new ArrayList<>(Arrays.asList(args));
            List<String> arguments = command.isQuoted() ? CommandParser.combineQuotedArguments(argumentList) : argumentList;

            Map<Character, String> flags = blade.getParser().parseFlags(command, arguments);
            for (Map.Entry<Character, String> entry : flags.entrySet()) {
                arguments.remove("-" + entry.getKey());

                boolean isFlag = command.getFlagParameters().stream().anyMatch(flag -> flag.getFlag().value() == entry.getKey());
                if (!isFlag || !"true".equals(entry.getValue())) arguments.remove(entry.getValue());
            }

            if (arguments.isEmpty()) return;
            if (command.getParameterProviders().size() < arguments.size()) return;

            int index = Math.max(0, arguments.size() - 1);
            String argument = index < arguments.size() ? arguments.get(index) : "";

            Parameter parameter = index < command.getParameters().size() ? command.getParameters().get(index) : null;
            ArgumentProvider<?> parameterProvider = parameter != null && parameter.hasCustomCompleter()
                ? parameter.getCustomCompleter()
                : command.getParameterProviders().get(index);

            if (parameterProvider == null) {
                throw new StacklessErrorMessage(
                    "Could not find argument provider for parameter '%s' (%s) for command '%s'.",
                    parameter != null ? parameter.getName() : ("@ index " + index),
                    parameter != null ? parameter.getType().getCanonicalName() : "unknown",
                    command.getAliases()[0]);
            }

            Argument bladeArgument = new Argument(parameter);
            bladeArgument.setType(index < arguments.size() ? Type.PROVIDED : Type.OPTIONAL);
            bladeArgument.setString(argument);

            if (parameter != null) {
                bladeArgument.getData().addAll(parameter.getData());

                if (parameter.getElement() != null) {
                    bladeArgument.addAnnotations(Arrays.asList(parameter.getElement().getAnnotations()));
                }
            }

            List<String> suggested = parameterProvider.suggest(context, bladeArgument);
            suggestions.addAll(suggested);
        } catch (BladeExitMessage ex) {
            throw ex;
        } catch (Throwable t) {
            blade.logger().error(
                t.getCause() != null ? t.getCause() : t,
                "An error occurred while parsing arguments for command '%s'.",
                command.getAliases()[0]
            );

            throw new BladeExitMessage("An error occurred while parsing your arguments.");
        }
    }

    public void suggestSubCommand(@NotNull List<String> suggestions,
                                  @NotNull String commandLine,
                                  @NotNull Supplier<WrappedSender<?>> senderSupplier) throws BladeExitMessage {
        String[] commandLineParts = commandLine.split(" ");
        if (commandLineParts.length == 0) return;
        String baseCommand = commandLineParts[0];

        List<Command> commandsWithBase = blade.getAliasToCommands().get(baseCommand);
        if (commandsWithBase == null) return;

        int currentWordIndex = commandLineParts.length - 1 + (commandLine.endsWith(" ") ? 1 : 0);
        if (currentWordIndex == 0) return;

        for (Command bladeCommand : commandsWithBase) {
            if (bladeCommand.isHidden()) continue;

            Context context = new Context(blade, senderSupplier.get(), bladeCommand.getAliases()[0], new String[0]);
            if (!blade.getPermissionTester().testPermission(context, bladeCommand)) continue;

            for (String alias : bladeCommand.getAliases()) {
                if (!alias.startsWith(commandLine.toLowerCase(Locale.ROOT))) continue;

                String[] aliasWords = alias.split(" ");
                if (aliasWords.length < currentWordIndex) continue;

                String currentWord = aliasWords[currentWordIndex];
                if (currentWord.isEmpty() || suggestions.contains(currentWord)) continue;

                suggestions.add(currentWord);
            }
        }
    }

}