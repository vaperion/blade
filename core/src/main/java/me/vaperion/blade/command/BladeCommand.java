package me.vaperion.blade.command;

import lombok.Getter;
import lombok.ToString;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.command.*;
import me.vaperion.blade.annotation.parameter.Data;
import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.annotation.parameter.Name;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.context.Sender;
import me.vaperion.blade.sender.internal.SndProvider;
import me.vaperion.blade.tokenizer.input.CommandInput;
import me.vaperion.blade.tokenizer.input.InputOption;
import me.vaperion.blade.util.BladeHelper;
import me.vaperion.blade.util.ClassUtil;
import me.vaperion.blade.util.LoadedValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static me.vaperion.blade.util.Preconditions.*;

@Getter
@ToString
public final class BladeCommand {

    private final Blade blade;

    private final Object instance;
    private final Method method;
    private final String[] labels, baseCommands;
    private final String description, mainLabel, customUsage, extraUsageData;
    private final String permission, permissionMessage;
    private final boolean async, parseQuotes, hidden;

    private final boolean hasSenderParameter, usesBladeContext, usesBladeSender;
    private final Class<?> senderType;

    private final List<BladeParameter> parameters = new ArrayList<>();

    private final List<ArgumentProvider<?>> rawProviderList = new ArrayList<>(),
        argumentProviders = new ArrayList<>(),
        flagProviders = new ArrayList<>();

    private final List<SndProvider<?>> senderProviders = new ArrayList<>();

    private final LoadedValue<InternalUsage<?>> usageMessage = new LoadedValue<>(),
        helpMessage = new LoadedValue<>();

    public BladeCommand(@NotNull Blade blade,
                        @Nullable Object instance,
                        @NotNull Method method) {
        this.blade = blade;

        this.instance = instance;
        this.method = method;

        this.labels = BladeHelper.generateLabels(
                mustGetAnnotation(method, Command.class),
                method.getDeclaringClass().getAnnotation(Command.class))
            .toArray(new String[0]);

        this.description = runOrDefault(method.getAnnotation(Description.class), "", Description::value);
        this.async = runOrDefault(method.getAnnotation(Async.class), false, $ -> true);
        this.hidden = runOrDefault(method.getAnnotation(Hidden.class), false, $ -> true);
        this.mainLabel = runOrDefault(method.getAnnotation(MainLabel.class), this.labels[0], MainLabel::value);
        this.customUsage = runOrDefault(method.getAnnotation(Usage.class), "", Usage::value);
        this.extraUsageData = runOrDefault(method.getAnnotation(ExtraUsage.class), "", ExtraUsage::value);

        this.baseCommands = Arrays.stream(this.labels)
            .map(String::toLowerCase)
            .map(s -> s.split(" ")[0])
            .distinct().toArray(String[]::new);

        Permission permission = method.getAnnotation(Permission.class);
        this.permission = permission != null ? permission.value() : "";
        this.permissionMessage = checkNotEmpty(permission != null ? permission.message() : "", blade.configuration().defaultPermissionMessage());

        this.parseQuotes = method.isAnnotationPresent(Quoted.class);

        this.hasSenderParameter = method.getParameterCount() > 0 && method.getParameters()[0].isAnnotationPresent(me.vaperion.blade.annotation.parameter.Sender.class);
        this.senderType = hasSenderParameter ? method.getParameters()[0].getType() : null;
        this.usesBladeContext = method.getParameterCount() == 1 && method.getParameterTypes()[0] == Context.class;
        this.usesBladeSender = method.getParameterCount() == 1 && method.getParameterTypes()[0] == Sender.class;

        method.setAccessible(true);

        int i = 0;
        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (i == 0 && hasSenderParameter) {
                for (SndProvider<?> provider : blade.senderProviders()) {
                    if (provider.type().isAssignableFrom(this.senderType)) {
                        senderProviders.add(provider);
                    }
                }

                // Sort providers so exact type match comes first
                senderProviders.sort((a, b) -> {
                    if (a.type().equals(this.senderType)) return -1;
                    if (b.type().equals(this.senderType)) return 1;
                    return 0;
                });

                i++;
                continue;
            }

            Class<?> type = ClassUtil.getGenericOrRawType(parameter);

            ArgumentProvider<?> provider = blade.providerResolver()
                .resolveRecursively(type,
                    Arrays.asList(parameter.getAnnotations()));

            String parameterName = parameter.isAnnotationPresent(Name.class)
                ? mustGetAnnotation(parameter, Name.class).value()
                : provider != null && provider.defaultArgName(parameter) != null
                ? Objects.requireNonNull(provider.defaultArgName(parameter))
                : parameter.getName();

            String[] parameterData = parameter.isAnnotationPresent(Data.class)
                ? mustGetAnnotation(parameter, Data.class).value()
                : null;

            BladeParameter bladeParameter;

            if (parameter.isAnnotationPresent(Flag.class)) {
                Flag flag = mustGetAnnotation(parameter, Flag.class);

                bladeParameter = new DefinedFlag(blade,
                    parameterName,
                    type,
                    parameter,
                    flag);
            } else {
                bladeParameter = new DefinedArgument(blade,
                    parameterName,
                    type,
                    parameterData == null
                        ? Collections.emptyList()
                        : Arrays.asList(parameterData),
                    parameter);
            }

            rawProviderList.add(provider);
            parameters.add(bladeParameter);

            if (bladeParameter instanceof DefinedFlag)
                flagProviders.add(provider);
            else
                argumentProviders.add(provider);

            i++;
        }
    }

    /**
     * Checks if any of the command's labels start with the given input string.
     *
     * @param input the string to check
     * @return true if any label starts with the input string, false otherwise
     */
    public boolean anyLabelStartsWith(@NotNull String input) {
        input = input.toLowerCase(Locale.ROOT);

        for (String label : labels) {
            if (label.toLowerCase(Locale.ROOT).startsWith(input)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given context has permission to execute this command.
     *
     * @param context the command context
     * @return true if the context has permission, false otherwise
     */
    public boolean hasPermission(@NotNull Context context) {
        return blade.permissionTester().testPermission(context, this);
    }

    /**
     * Tokenizes the given input string according to this command's parameters.
     *
     * @param sender  the sender executing the command
     * @param input   the input string to tokenize
     * @param options the input options to use during tokenization
     * @return the tokenized command input
     */
    @NotNull
    public CommandInput tokenize(@NotNull Sender<?> sender,
                                 @NotNull String input,
                                 @NotNull InputOption... options) {
        CommandInput commandInput = new CommandInput(blade, this, input, options);
        commandInput.tokenize();
        return commandInput;
    }

    /**
     * Get all argument parameters.
     *
     * @return a list of argument parameters
     */
    @NotNull
    public List<DefinedArgument> arguments() {
        return parameters.stream()
            .filter(DefinedArgument.class::isInstance)
            .map(DefinedArgument.class::cast)
            .collect(Collectors.toList());
    }

    /**
     * Get all flag parameters.
     *
     * @return a list of flag parameters
     */
    @NotNull
    public List<DefinedFlag> flags() {
        return parameters.stream()
            .filter(DefinedFlag.class::isInstance)
            .map(DefinedFlag.class::cast)
            .collect(Collectors.toList());
    }

}
