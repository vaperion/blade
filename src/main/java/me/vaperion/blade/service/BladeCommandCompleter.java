package me.vaperion.blade.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.argument.BladeArgument;
import me.vaperion.blade.argument.BladeProvider;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.BladeParameter;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.context.WrappedSender;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.utils.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class BladeCommandCompleter {

    private final BladeCommandService commandService;

    @Nullable
    public List<String> suggest(@NotNull String commandLine, @NotNull Supplier<WrappedSender<?>> senderSupplier,
                                @NotNull Function<BladeCommand, Boolean> permissionFunction) {
        List<String> suggestions = new ArrayList<>();
        suggestSubCommand(suggestions, commandLine, permissionFunction);

        String[] commandParts = commandLine.split(" ");
        Tuple<BladeCommand, String> resolved = commandService.getCommandResolver().resolveCommand(commandParts);

        if (resolved == null) return suggestions.isEmpty() ? null : suggestions;
        if (!permissionFunction.apply(resolved.getLeft()) || resolved.getLeft().isContextBased()) return suggestions.isEmpty() ? null : suggestions;

        BladeCommand command = resolved.getLeft();
        String foundAlias = resolved.getRight();

        List<String> argList = new ArrayList<>(Arrays.asList(commandParts));
        argList.subList(0, Math.max(1, foundAlias.split(" ").length)).clear();

        if (commandLine.endsWith(" ")) argList.add("");
        String[] actualArguments = argList.toArray(new String[0]);

        BladeContext context = new BladeContext(commandService, senderSupplier.get(), foundAlias, actualArguments);

        suggest(suggestions, context, command, actualArguments);
        return suggestions;
    }

    public void suggest(@NotNull List<String> suggestions, @NotNull BladeContext context,
                        @NotNull BladeCommand command, @NotNull String[] args) throws BladeExitMessage {
        if (command.isContextBased()) return;

        try {
            List<String> argumentList = new ArrayList<>(Arrays.asList(args));
            List<String> arguments = command.isQuoted() ? commandService.getCommandParser().combineQuotedArguments(argumentList) : argumentList;

            Map<Character, String> flags = commandService.getCommandParser().parseFlags(command, arguments);
            for (Map.Entry<Character, String> entry : flags.entrySet()) {
                arguments.remove("-" + entry.getKey());

                boolean isFlag = command.getFlagParameters().stream().anyMatch(flag -> flag.getFlag().value() == entry.getKey());
                if (!isFlag || !"true".equals(entry.getValue())) arguments.remove(entry.getValue());
            }

            if (arguments.size() == 0) return;
            if (command.getParameterProviders().size() < arguments.size()) return;

            int index = Math.max(0, arguments.size() - 1);
            String argument = index < arguments.size() ? arguments.get(index) : "";

            BladeParameter parameter = index < command.getParameters().size() ? command.getParameters().get(index) : null;
            BladeProvider<?> parameterProvider = parameter != null && parameter.hasCustomTabCompleter()
                  ? parameter.getCompleter() : command.getParameterProviders().get(index);

            if (parameterProvider == null) {
                throw new BladeExitMessage("Could not find provider for argument " + index + ".");
            }

            BladeArgument bladeArgument = new BladeArgument(parameter);
            bladeArgument.setType(index < arguments.size() ? BladeArgument.Type.PROVIDED : BladeArgument.Type.OPTIONAL);
            bladeArgument.setString(argument);
            if (parameter != null) bladeArgument.getData().addAll(parameter.getData());

            List<String> suggested = parameterProvider.suggest(context, bladeArgument);
            suggestions.addAll(suggested);
        } catch (BladeExitMessage ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BladeExitMessage("An exception was thrown while parsing your arguments.");
        }
    }

    public void suggestSubCommand(@NotNull List<String> suggestions, @NotNull String commandLine,
                                  @NotNull Function<BladeCommand, Boolean> permissionFunction) throws BladeExitMessage {
        String[] commandLineParts = commandLine.split(" ");
        String baseCommand = commandLineParts[0];

        List<BladeCommand> commandsWithBase = commandService.aliasCommands.get(baseCommand);
        if (commandsWithBase == null) return;

        int currentWordIndex = commandLineParts.length - 1 + (commandLine.endsWith(" ") ? 1 : 0);
        if (currentWordIndex == 0) return;

        for (BladeCommand bladeCommand : commandsWithBase) {
            if (!permissionFunction.apply(bladeCommand)) continue;

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
