package me.vaperion.blade.command.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.container.BladeCommand;
import me.vaperion.blade.command.context.BladeContext;
import me.vaperion.blade.command.context.WrappedSender;
import me.vaperion.blade.command.exception.BladeExitMessage;
import me.vaperion.blade.utils.Tuple;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class BladeCommandCompleter {

    private final BladeCommandService commandService;

    @NotNull
    public List<String> suggest(@NotNull String commandLine, @NotNull Supplier<WrappedSender<?>> senderSupplier, @NotNull Function<BladeCommand, Boolean> permissionFunction) {
        String[] commandParts = commandLine.split(" ");

        Tuple<BladeCommand, String> resolved = commandService.getCommandResolver().resolveCommand(commandParts);
        if (resolved == null) return Collections.emptyList();
        if (!permissionFunction.apply(resolved.getLeft())) return Collections.emptyList();

        BladeCommand command = resolved.getLeft();
        String foundAlias = resolved.getRight();

        List<String> argList = new ArrayList<>(Arrays.asList(commandParts));
        if (foundAlias.split(" ").length > 1) argList.subList(0, foundAlias.split(" ").length).clear();

        if (commandLine.endsWith(" ")) argList.add("");
        String[] actualArguments = argList.toArray(new String[0]);

        BladeContext context = new BladeContext(senderSupplier.get(), foundAlias, actualArguments);
        return commandService.getCommandCompleter().suggest(context, command, actualArguments);
    }

    @NotNull
    public List<String> suggest(@NotNull BladeContext context, @NotNull BladeCommand command, @NotNull String[] args) throws BladeExitMessage {
        try {
            List<String> argumentList = new ArrayList<>(Arrays.asList(args));
            List<String> arguments = command.isQuoted() ? commandService.getCommandParser().combineQuotedArguments(argumentList) : argumentList;

            Map<Character, String> flags = commandService.getCommandParser().parseFlags(command, arguments);
            for (Map.Entry<Character, String> entry : flags.entrySet()) {
                arguments.remove("-" + entry.getKey());

                boolean isFlag = command.getFlagParameters().stream().anyMatch(flag -> flag.getFlag().value() == entry.getKey());
                if (!isFlag || !"true".equals(entry.getValue())) arguments.remove(entry.getValue());
            }

            if (arguments.size() == 0) return Collections.emptyList();
            if (command.getParameterProviders().size() < arguments.size()) return Collections.emptyList();

            int index = Math.max(0, arguments.size() - 1);
            BladeProvider<?> parameterProvider = command.getParameterProviders().get(index);
            String argument = index < arguments.size() ? arguments.get(index) : "";

            return parameterProvider.suggest(context, argument);
        } catch (BladeExitMessage ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BladeExitMessage("An exception was thrown while parsing your arguments.");
        }
    }

}
