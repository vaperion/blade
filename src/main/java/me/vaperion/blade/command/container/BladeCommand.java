package me.vaperion.blade.command.container;

import lombok.Getter;
import me.vaperion.blade.command.annotation.*;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.context.BladeContext;
import me.vaperion.blade.command.service.BladeCommandService;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class BladeCommand {
    private final BladeCommandService commandService;

    private final Object instance;
    private final Method method;
    private final String[] aliases, realAliases;
    private final String description, extraUsageData;
    private final String permission, permissionMessage;
    private final boolean async, quoted;

    private final boolean contextBased;

    private final Class<?> senderType;
    private final boolean senderParameter;

    private final List<BladeParameter> parameters = new LinkedList<>();
    private final List<BladeProvider<?>> providers = new LinkedList<>(), parameterProviders = new LinkedList<>(), flagProviders = new LinkedList<>();

    public BladeCommand(BladeCommandService commandService, Object instance, Method method, String[] aliases, Command command, Permission permission) {
        this.commandService = commandService;

        this.instance = instance;
        this.method = method;
        this.aliases = aliases;
        this.realAliases = Arrays.stream(aliases).map(String::toLowerCase).map(s -> s.split(" ")[0]).distinct().toArray(String[]::new);
        this.description = command.description();
        this.extraUsageData = command.extraUsageData();
        this.async = command.async();
        this.quoted = command.quoted();

        this.permission = permission == null ? "" : permission.value();
        this.permissionMessage = permission == null ? "" : permission.message();

        this.contextBased = method != null && method.getParameterCount() == 1 && method.getParameterTypes()[0] == BladeContext.class;

        this.senderParameter = method != null && method.getParameterCount() > 0 && method.getParameters()[0].isAnnotationPresent(Sender.class);
        this.senderType = this.senderParameter ? method.getParameterTypes()[0] : null;

        if (method != null) {
            int i = 0;
            for (Parameter parameter : method.getParameters()) {
                if (i == 0 && senderParameter) {
                    i++;
                    continue;
                }

                String parameterName = parameter.isAnnotationPresent(Name.class) ? parameter.getAnnotation(Name.class).value() : parameter.getName();
                BladeParameter bladeParameter;

                if (parameter.isAnnotationPresent(Flag.class)) {
                    Flag flag = parameter.getAnnotation(Flag.class);
                    bladeParameter = new BladeParameter.FlagParameter(parameterName, parameter.getType(), parameter.getAnnotation(Optional.class), flag);
                } else {
                    bladeParameter = new BladeParameter.CommandParameter(parameterName, parameter.getType(), parameter.getAnnotation(Optional.class), parameter.getAnnotation(Range.class), parameter.isAnnotationPresent(Combined.class));
                }

                BladeProvider<?> provider = commandService.getCommandResolver().resolveProvider(parameter.getType(), Arrays.asList(parameter.getAnnotations()));

                parameters.add(bladeParameter);
                providers.add(provider);

                if (bladeParameter instanceof BladeParameter.FlagParameter)
                    flagProviders.add(provider);
                else
                    parameterProviders.add(provider);

                i++;
            }
        }
    }

    @NotNull
    public List<BladeParameter.CommandParameter> getCommandParameters() {
        return parameters.stream()
                .filter(BladeParameter.CommandParameter.class::isInstance)
                .map(BladeParameter.CommandParameter.class::cast)
                .collect(Collectors.toList());
    }

    @NotNull
    public List<BladeParameter.FlagParameter> getFlagParameters() {
        return parameters.stream()
                .filter(BladeParameter.FlagParameter.class::isInstance)
                .map(BladeParameter.FlagParameter.class::cast)
                .collect(Collectors.toList());
    }
}
