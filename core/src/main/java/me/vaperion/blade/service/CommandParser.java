package me.vaperion.blade.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.argument.Argument;
import me.vaperion.blade.argument.Argument.Type;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.Parameter;
import me.vaperion.blade.command.Parameter.FlagParameter;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.exception.BladeUsageMessage;
import me.vaperion.blade.exception.StacklessErrorMessage;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@RequiredArgsConstructor
public class CommandParser {

    private final Blade blade;

    @NotNull
    public List<Object> parseArguments(@NotNull BladeCommand command,
                                       @NotNull Context context,
                                       @NotNull String[] argArray) throws BladeExitMessage {
        List<String> args = new ArrayList<>(Arrays.asList(argArray));
        List<Object> result = new ArrayList<>(command.getParameters().size());

        try {
            List<String> arguments = command.isQuoted()
                ? combineQuotedArguments(args)
                : args;

            Map<Character, String> flags = parseFlags(command, arguments);

            int argIndex = 0, providerIndex = 0;
            for (Parameter parameter : command.getParameters()) {
                boolean flag = parameter instanceof FlagParameter;
                Argument bladeArgument = new Argument(parameter);

                String data;
                if (!flag) {
                    if (arguments.size() > argIndex) {
                        data = arguments.get(argIndex);
                        bladeArgument.setType(Type.PROVIDED);
                    } else if (parameter.isOptional()) {
                        data = parameter.getDefault();
                        bladeArgument.setType(Type.OPTIONAL);
                    } else throw new BladeUsageMessage();

                    if (parameter.isText())
                        data = arguments.size() > argIndex ? String.join(" ", arguments.subList(argIndex, arguments.size())) : data;
                } else data = ((FlagParameter) parameter).extractFrom(flags);
                bladeArgument.setString(data);
                bladeArgument.getData().addAll(parameter.getData());

                try {
                    ArgumentProvider<?> provider = command.getProviders().get(providerIndex);
                    if (provider == null) {
                        throw new StacklessErrorMessage(
                            "Could not find argument provider for parameter '%s' (%s) for command '%s'.",
                            parameter.getName(), parameter.getType().getCanonicalName(), command.getAliases()[0]);
                    }

                    Object parsed;
                    if (bladeArgument.getType() == Type.OPTIONAL && bladeArgument.getParameter().defaultsToNull())
                        parsed = null;
                    else
                        parsed = provider.provide(context, bladeArgument);
                    result.add(parsed);

                    if (parsed == null && !parameter.defaultsToNull() && !parameter.ignoreFailedArgumentParse())
                        throw new BladeUsageMessage();

                    if (!flag) argIndex++;
                    providerIndex++;
                } catch (BladeExitMessage ex) {
                    throw ex;
                } catch (Throwable t) {
                    blade.logger().error(
                        t.getCause() != null ? t.getCause() : t,
                        "An error occurred while parsing argument '%s' of type '%s' for command '%s'.",
                        parameter.getName(), parameter.getType().getCanonicalName(), command.getAliases()[0]);

                    throw new BladeExitMessage("Failed to parse one of your arguments.");
                }
            }

            return result;
        } catch (BladeExitMessage ex) {
            throw ex;
        } catch (Throwable t) {
            blade.logger().error(
                t.getCause() != null ? t.getCause() : t,
                "An error occurred while parsing arguments for command '%s'.",
                command.getAliases()[0]);

            throw new BladeExitMessage("An error occurred while parsing your arguments.");
        }
    }

    @NotNull
    public Map<Character, String> parseFlags(@NotNull BladeCommand command, @NotNull List<String> args) throws BladeExitMessage {
        Map<Character, String> map = new LinkedHashMap<>();
        Character pendingFlag = null;

        Iterator<String> it = args.iterator();
        while (it.hasNext()) {
            String arg = it.next();

            if (pendingFlag != null) {
                it.remove();
                map.put(pendingFlag, arg);
                pendingFlag = null;
            }

            if (arg.length() == 2 && arg.charAt(0) == '-') {
                char flag = arg.charAt(1);

                FlagParameter flagParameter = command.getFlagParameters().stream()
                    .filter(param -> param.getFlag().value() == flag)
                    .findFirst().orElse(null);
                if (flagParameter == null) continue;

                it.remove();

                if (flagParameter.isBooleanFlag())
                    map.put(flag, "true");
                else pendingFlag = flag;
            }
        }

        if (pendingFlag != null)
            throw new BladeExitMessage("The '-" + pendingFlag + "' flag requires a value.");

        return map;
    }

    @NotNull
    public static List<String> combineQuotedArguments(@NotNull List<String> args) {
        String whole = String.join(" ", args);
        List<String> arguments = new ArrayList<>(args.size());

        char boundary = '\0';
        StringBuilder building = new StringBuilder();

        for (char c : whole.toCharArray()) {
            if (c == '"' || c == '\'') {
                if (boundary == '\0') {
                    boundary = c;
                    building.setLength(0);
                    continue;
                }

                if (boundary == c) {
                    boundary = '\0';
                    arguments.add(building.toString());
                    building.setLength(0);
                    continue;
                }
            }

            if (c == ' ' && boundary == '\0') {
                if (building.length() > 0) {
                    arguments.add(building.toString());
                    building.setLength(0);
                }
                continue;
            }

            building.append(c);
        }

        if (building.length() > 0) {
            arguments.add(building.toString());
        }

        return arguments;
    }

}
