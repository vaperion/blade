package me.vaperion.blade.command.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.container.BladeCommand;
import me.vaperion.blade.command.container.BladeParameter;
import me.vaperion.blade.command.exception.BladeExitMessage;
import me.vaperion.blade.utils.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class BladeCommandCompleter {

    private final BladeCommandService commandService;

    @Nullable
    public Tuple<BladeProvider<?>, String> getLastProvider(@NotNull BladeCommand command, @NotNull String[] args) throws BladeExitMessage {
        BladeProvider<?> lastProvider = null;
        String lastArgument = null;

        try {
            List<String> argumentList = new ArrayList<>(Arrays.asList(args));
            List<String> arguments = command.isQuoted() ? commandService.getCommandParser().combineQuotedArguments(argumentList) : argumentList;

            int argIndex = 0, providerIndex = 0;
            for (BladeParameter parameter : command.getParameters()) {
                boolean flag = false;

                if (parameter instanceof BladeParameter.FlagParameter) {
                    flag = true;
                } else {
                    if (arguments.size() <= argIndex) return new Tuple<>(lastProvider, lastArgument);
                }

                BladeProvider<?> provider = command.getProviders().get(providerIndex);
                if (provider == null)
                    throw new BladeExitMessage("Could not find provider for type '" + parameter.getType().getCanonicalName() + "'.");

                lastProvider = provider;
                lastArgument = (arguments.size() - 1 >= argIndex) ? arguments.get(argIndex) : null;

                if (!flag) argIndex++;
                providerIndex++;
            }

            return new Tuple<>(lastProvider, lastArgument);
        } catch (BladeExitMessage ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BladeExitMessage("An exception was thrown while parsing your arguments.");
        }
    }

}
