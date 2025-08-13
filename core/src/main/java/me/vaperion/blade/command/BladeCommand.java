package me.vaperion.blade.command;

import lombok.Getter;
import lombok.ToString;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.argument.*;
import me.vaperion.blade.annotation.command.*;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.command.Parameter.CommandParameter;
import me.vaperion.blade.command.Parameter.FlagParameter;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.context.WrappedSender;
import me.vaperion.blade.sender.internal.SndProvider;
import me.vaperion.blade.util.BladeHelper;
import me.vaperion.blade.util.ClassUtil;
import me.vaperion.blade.util.LoadedValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static me.vaperion.blade.util.Preconditions.*;

@Getter
@ToString
public final class BladeCommand {

    private final Blade blade;

    private final Object instance;
    private final Method method;
    private final String[] aliases, baseCommands;
    private final String description, usageAlias, customUsage, extraUsageData;
    private final String permission, permissionMessage;
    private final boolean async, quoted, hidden;

    private final boolean hasSenderParameter, contextBased, wrappedSenderBased;
    private final Class<?> senderType;

    private final List<Parameter> parameters = new ArrayList<>();

    private final List<ArgumentProvider<?>> providers = new ArrayList<>(),
        parameterProviders = new ArrayList<>(),
        flagProviders = new ArrayList<>();

    private final List<SndProvider<?>> senderProviders = new ArrayList<>();

    private final LoadedValue<UsageMessage> usageMessage = new LoadedValue<>(), helpMessage = new LoadedValue<>();

    public BladeCommand(@NotNull Blade blade,
                        @Nullable Object instance,
                        @NotNull Method method) {
        this.blade = blade;

        this.instance = instance;
        this.method = method;

        this.aliases = BladeHelper.generateAliases(
                mustGetAnnotation(method, Command.class),
                method.getDeclaringClass().getAnnotation(Command.class))
            .toArray(new String[0]);

        this.description = runOrDefault(method.getAnnotation(Description.class), "", Description::value);
        this.async = runOrDefault(method.getAnnotation(Async.class), false, $ -> true);
        this.hidden = runOrDefault(method.getAnnotation(Hidden.class), false, $ -> true);
        this.usageAlias = runOrDefault(method.getAnnotation(UsageAlias.class), this.aliases[0], UsageAlias::value);
        this.customUsage = runOrDefault(method.getAnnotation(Usage.class), "", Usage::value);
        this.extraUsageData = runOrDefault(method.getAnnotation(ExtraUsage.class), "", ExtraUsage::value);

        this.baseCommands = Arrays.stream(this.aliases)
            .map(String::toLowerCase)
            .map(s -> s.split(" ")[0])
            .distinct().toArray(String[]::new);

        Permission permission = method.getAnnotation(Permission.class);
        this.permission = permission != null ? permission.value() : "";
        this.permissionMessage = checkNotEmpty(permission != null ? permission.message() : "", blade.getConfiguration().getDefaultPermissionMessage());

        this.quoted = method.isAnnotationPresent(ParseQuotes.class);

        this.hasSenderParameter = method.getParameterCount() > 0 && method.getParameters()[0].isAnnotationPresent(Sender.class);
        this.senderType = hasSenderParameter ? method.getParameters()[0].getType() : null;
        this.contextBased = method.getParameterCount() == 1 && method.getParameterTypes()[0] == Context.class;
        this.wrappedSenderBased = method.getParameterCount() == 1 && method.getParameterTypes()[0] == WrappedSender.class;

        method.setAccessible(true);

        int i = 0;
        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (i == 0 && hasSenderParameter) {
                for (SndProvider<?> provider : blade.getSenderProviders()) {
                    if (provider.getType().isAssignableFrom(this.senderType)) {
                        senderProviders.add(provider);
                    }
                }

                // Sort providers so exact type match comes first
                senderProviders.sort((a, b) -> {
                    if (a.getType().equals(this.senderType)) return -1;
                    if (b.getType().equals(this.senderType)) return 1;
                    return 0;
                });

                i++;
                continue;
            }

            Class<?> type = ClassUtil.getGenericOrRawType(parameter);

            String parameterName = parameter.isAnnotationPresent(Name.class)
                ? mustGetAnnotation(parameter, Name.class).value() : parameter.getName();
            String[] parameterData = parameter.isAnnotationPresent(Data.class)
                ? mustGetAnnotation(parameter, Data.class).value() : null;

            Parameter bladeParameter;

            if (parameter.isAnnotationPresent(Flag.class)) {
                Flag flag = mustGetAnnotation(parameter, Flag.class);

                bladeParameter = new FlagParameter(parameterName,
                    type,
                    parameter,
                    flag,
                    parameter.getAnnotation(Optional.class));
            } else {
                bladeParameter = new CommandParameter(parameterName,
                    type,
                    parameterData == null
                        ? Collections.emptyList()
                        : Arrays.asList(parameterData),
                    parameter.getAnnotation(Optional.class),
                    parameter.getAnnotation(Range.class),
                    parameter.getAnnotation(Completer.class),
                    parameter.isAnnotationPresent(Text.class),
                    parameter);
            }

            ArgumentProvider<?> provider = blade.getResolver()
                .recursiveResolveProvider(type,
                    Arrays.asList(parameter.getAnnotations()));

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
