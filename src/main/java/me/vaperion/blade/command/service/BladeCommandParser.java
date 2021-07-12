package me.vaperion.blade.command.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.container.BladeCommand;
import me.vaperion.blade.command.container.BladeParameter;
import me.vaperion.blade.command.context.BladeContext;
import me.vaperion.blade.command.exception.BladeExitMessage;
import me.vaperion.blade.command.exception.BladeUsageMessage;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BladeCommandParser {

    private final BladeCommandService commandService;

    @NotNull
    public List<Object> parseArguments(@NotNull BladeCommand command, @NotNull BladeContext context, @NotNull String[] args) throws BladeExitMessage {
        List<Object> result = new ArrayList<>(command.getParameters().size());

        try {
            List<String> argumentList = new ArrayList<>(Arrays.asList(args));
            List<String> arguments = command.isQuoted() ? combineQuotedArguments(argumentList) : argumentList;
            Map<Character, String> flags = parseFlags(command, arguments);

            if (arguments.size() < command.getParameters().size()) { // append default values to the end
                List<BladeParameter> realParameters = command.getParameters().stream()
                        .filter(p -> !(p instanceof BladeParameter.FlagParameter))
                        .collect(Collectors.toList());

                for (BladeParameter parameter : realParameters.subList(arguments.size(), realParameters.size())) {
                    String defaultValue = parameter.getDefault();
                    if (defaultValue == null && parameter.getType() == String.class) continue;
//                    if (parameter.isCombined()) continue;

                    arguments.add(defaultValue);
                }
            }

            int argIndex = 0, providerIndex = 0;
            for (BladeParameter parameter : command.getParameters()) {
                String data;
                boolean flag = false;

                if (parameter instanceof BladeParameter.FlagParameter) {
                    data = ((BladeParameter.FlagParameter) parameter).extractFrom(flags);
                    flag = true;
                } else {
                    if (arguments.size() <= argIndex) throw new BladeUsageMessage();

                    if (parameter.isCombined()) data = String.join(" ", arguments.subList(argIndex, arguments.size()));
                    else data = arguments.get(argIndex);
                }

                try {
                    BladeProvider<?> provider = command.getProviders().get(providerIndex);
                    if (provider == null)
                        throw new BladeExitMessage("Could not find provider for type '" + parameter.getType().getCanonicalName() + "'.");

                    Object parsed = provider.provide(context, parameter, data);
                    result.add(parsed);

                    if (parsed == null && !parameter.allowsNull())
                        throw new BladeUsageMessage();

                    if (!flag) argIndex++;
                    providerIndex++;
                } catch (BladeExitMessage ex) {
                    throw ex;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new BladeExitMessage("Failed to parse one of your arguments.");
                }
            }

            return result;
        } catch (BladeExitMessage ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BladeExitMessage("An exception was thrown while parsing your arguments.");
        }
    }

    @NotNull
    public Map<Character, String> parseFlags(@NotNull BladeCommand command, @NotNull List<String> args) throws BladeExitMessage {
        Map<Character, String> map = new LinkedHashMap<>();
        Character pendingFlag = null;

        Iterator<String> it = args.iterator();
        while (it.hasNext()) {
            String arg = it.next();

            if (arg.length() == 2 && arg.charAt(0) == '-') {
                char flag = arg.charAt(1);

                BladeParameter.FlagParameter flagParameter = command.getFlagParameters().stream()
                        .filter(param -> param.getFlag().value() == flag)
                        .findFirst().orElse(null);
                if (flagParameter == null) continue;

                it.remove();

                if (flagParameter.isBooleanFlag())
                    map.put(flag, "true");
                else if (pendingFlag != null)
                    throw new BladeExitMessage("Invalid flag usage.");
                else
                    pendingFlag = flag;
                continue;
            }

            if (pendingFlag != null) {
                it.remove();
                map.put(pendingFlag, arg);
                pendingFlag = null;
            }
        }

        return map;
    }

    @NotNull
    public List<String> combineQuotedArguments(@NotNull List<String> args) {
        List<String> argList = new ArrayList<>(args.size());

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.isEmpty()) {
                argList.add(arg);
                continue;
            }

            char c = arg.charAt(0);
            if (c == '"' || c == '\'') {
                StringBuilder builder = new StringBuilder();

                int endIndex;
                for (endIndex = i; endIndex < args.size(); endIndex++) {
                    String endArg = args.get(endIndex);
                    if (endArg.charAt(endArg.length() - 1) == c && endArg.length() > 1) {
                        if (endIndex != i) {
                            builder.append(' ');
                        }
                        builder.append(endArg, endIndex == i ? 1 : 0, endArg.length() - 1);
                        break;
                    } else if (endIndex == i) {
                        builder.append(endArg.substring(1));
                    } else {
                        builder.append(' ').append(endArg);
                    }
                }

                if (endIndex < args.size()) {
                    arg = builder.toString();
                    i = endIndex;
                }
            }

            argList.add(arg);
        }

        return argList;
    }

}
