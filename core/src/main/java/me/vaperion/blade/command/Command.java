package me.vaperion.blade.command;

import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.*;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.command.Parameter.CommandParameter;
import me.vaperion.blade.command.Parameter.FlagParameter;
import me.vaperion.blade.util.LoadedValue;
import me.vaperion.blade.util.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public final class Command {

    private final Blade blade;

    private final Object instance;
    private final Method method;
    private final String[] aliases, baseCommands;
    private final String description, usageAlias, customUsage, extraUsageData;
    private final String permission, permissionMessage;
    private final boolean async, quoted, hidden;

    private final boolean hasSenderParameter, contextBased;
    private final Class<?> senderType;

    private final List<Parameter> parameters = new ArrayList<>();
    private final List<ArgumentProvider<?>> providers = new ArrayList<>(), parameterProviders = new ArrayList<>(), flagProviders = new ArrayList<>();

    private final LoadedValue<UsageMessage> usageMessage = new LoadedValue<>();

    public Command(Blade blade, Object instance, Method method, String[] aliases) {
        this.blade = blade;

        this.instance = instance;
        this.method = method;
        this.aliases = aliases;
        this.baseCommands = Arrays.stream(aliases).map(String::toLowerCase).map(s -> s.split(" ")[0]).distinct().toArray(String[]::new);

        me.vaperion.blade.annotation.Command command = method.getAnnotation(me.vaperion.blade.annotation.Command.class);
        this.description = command.description();
        this.async = command.async();
        this.hidden = command.hidden();
        this.usageAlias = command.usageAlias();
        this.customUsage = command.customUsage();
        this.extraUsageData = command.extraUsageData();

        Permission permission = method.getAnnotation(Permission.class);
        this.permission = permission != null ? permission.value() : "";
        this.permissionMessage = Preconditions.checkNotEmpty(permission != null ? permission.message() : "", blade.getConfiguration().getDefaultPermissionMessage());

        this.quoted = method.isAnnotationPresent(ParseQuotes.class);

        this.hasSenderParameter = method.getParameterCount() > 0 && method.getParameters()[0].isAnnotationPresent(Sender.class);
        this.senderType = hasSenderParameter ? method.getParameters()[0].getType() : null;
        this.contextBased = method.getParameterCount() == 1 && method.getParameterTypes()[0] == this.senderType;

        method.setAccessible(true);

        int i = 0;
        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (i == 0 && hasSenderParameter) {
                i++;
                continue;
            }

            String parameterName = parameter.isAnnotationPresent(Name.class) ? parameter.getAnnotation(Name.class).value() : parameter.getName();
            String[] parameterData = parameter.isAnnotationPresent(Data.class) ? parameter.getAnnotation(Data.class).value() : null;
            Parameter bladeParameter;

            if (parameter.isAnnotationPresent(Flag.class)) {
                Flag flag = parameter.getAnnotation(Flag.class);
                bladeParameter = new FlagParameter(parameterName, parameter.getType(), parameter.getAnnotation(Optional.class), parameter, flag);
            } else {
                bladeParameter = new CommandParameter(parameterName, parameter.getType(),
                      parameterData == null ? Collections.emptyList() : Arrays.asList(parameterData), parameter.getAnnotation(Optional.class),
                      parameter.getAnnotation(Range.class), parameter.getAnnotation(Completer.class), parameter.isAnnotationPresent(Text.class), parameter);
            }

            ArgumentProvider<?> provider = blade.getResolver().resolveProvider(parameter.getType(), Arrays.asList(parameter.getAnnotations()));

            parameters.add(bladeParameter);
            providers.add(provider);

            if (bladeParameter instanceof FlagParameter)
                flagProviders.add(provider);
            else
                parameterProviders.add(provider);

            i++;
        }
    }

    @NotNull
    public List<CommandParameter> getCommandParameters() {
        return parameters.stream()
              .filter(CommandParameter.class::isInstance)
              .map(CommandParameter.class::cast)
              .collect(Collectors.toList());
    }

    @NotNull
    public List<FlagParameter> getFlagParameters() {
        return parameters.stream()
              .filter(FlagParameter.class::isInstance)
              .map(FlagParameter.class::cast)
              .collect(Collectors.toList());
    }

}
