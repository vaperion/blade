package me.vaperion.blade.command.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.container.BladeCommand;
import me.vaperion.blade.command.exception.BladeExitMessage;
import me.vaperion.blade.utils.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class BladeCommandCompleter {

    private final BladeCommandService commandService;

    @Nullable
    public Tuple<BladeProvider<?>, String> getLastProvider(@NotNull BladeCommand command, @NotNull String[] args) throws BladeExitMessage {
        try {
            List<String> argumentList = new ArrayList<>(Arrays.asList(args));
            System.out.println("Argument list: " + argumentList);
            List<String> arguments = command.isQuoted() ? commandService.getCommandParser().combineQuotedArguments(argumentList) : argumentList;
            System.out.println("Argument list 2: " + arguments);

            Map<Character, String> flags = commandService.getCommandParser().parseFlags(command, arguments);
            for (Map.Entry<Character, String> entry : flags.entrySet()) {
                arguments.remove(String.valueOf(entry.getKey()));
                if (!"true".equals(entry.getValue())) arguments.remove(entry.getValue());
            }

            System.out.println("Argument list 3: " + argumentList);

            if (arguments.size() == 0) return new Tuple<>();
            if (command.getParameterProviders().size() < arguments.size()) return new Tuple<>();

            int index = Math.max(0, arguments.size() - 1);
            return new Tuple<>(command.getParameterProviders().get(index), index < arguments.size() ? arguments.get(index) : null);
        } catch (BladeExitMessage ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BladeExitMessage("An exception was thrown while parsing your arguments.");
        }
    }

}
