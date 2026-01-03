package me.vaperion.blade.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.command.*;
import me.vaperion.blade.annotation.parameter.Data;
import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.annotation.parameter.Greedy;
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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
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
    private final boolean async, parseQuotes, hidden, helpCommand;

    private final boolean hasSenderParameter, usesBladeContext, usesBladeSender;
    private final Class<?> senderType;

    private final List<BladeParameter> parameters = new ArrayList<>();

    private final List<ArgumentProvider<?>> rawProviderList = new ArrayList<>(),
        argumentProviders = new ArrayList<>(),
        flagProviders = new ArrayList<>();

    private final List<SndProvider<?>> senderProviders = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private final LoadedValue<CommandFeedback<?>> usageMessage = new LoadedValue<>();
    @Getter(AccessLevel.NONE)
    private final LoadedValue<CommandFeedback<?>> helpMessage = new LoadedValue<>();

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

        this.helpCommand = runOrDefault(method.getAnnotation(Help.class), false, $ -> true);

        if (this.helpCommand) {
            // If this is a help command, ignore the actual method,
            // as Blade will handle it internally.

            this.parseQuotes = false;

            this.hasSenderParameter = false;
            this.senderType = null;
            this.usesBladeContext = false;
            this.usesBladeSender = false;

            // We add a "fake" greedy string parameter to capture all input for help commands
            BladeParameter bladeParameter = new DefinedArgument(blade,
                "query",
                String.class,
                Collections.emptyList(),
                createHelpGreedyArgument());

            ArgumentProvider<?> provider = blade.providerResolver()
                .resolveRecursively(String.class, Collections.emptyList());

            this.rawProviderList.add(provider);
            this.parameters.add(bladeParameter);
            this.argumentProviders.add(provider);

            return;
        }

        this.parseQuotes = method.isAnnotationPresent(Quoted.class);

        this.hasSenderParameter = method.getParameterCount() > 0 && method.getParameters()[0].isAnnotationPresent(me.vaperion.blade.annotation.parameter.Sender.class);
        this.senderType = hasSenderParameter ? method.getParameters()[0].getType() : null;
        this.usesBladeContext = method.getParameterCount() == 1 && method.getParameterTypes()[0] == Context.class;
        this.usesBladeSender = method.getParameterCount() == 1 && method.getParameterTypes()[0] == Sender.class;

        method.setAccessible(true);

        int i = 0;
        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            if (i == 0 && this.hasSenderParameter) {
                for (SndProvider<?> provider : blade.senderProviders()) {
                    if (provider.type().isAssignableFrom(this.senderType)) {
                        this.senderProviders.add(provider);
                    }
                }

                // Sort providers so exact type match comes first
                this.senderProviders.sort((a, b) -> {
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

            this.rawProviderList.add(provider);

            String parameterName = parameter.isAnnotationPresent(Name.class)
                ? mustGetAnnotation(parameter, Name.class).value()
                : provider != null && provider.defaultArgName(parameter) != null
                ? Objects.requireNonNull(provider.defaultArgName(parameter))
                : parameter.getName();

            String[] parameterData = parameter.isAnnotationPresent(Data.class)
                ? mustGetAnnotation(parameter, Data.class).value()
                : null;

            if (parameter.isAnnotationPresent(Flag.class)) {
                Flag flag = mustGetAnnotation(parameter, Flag.class);

                DefinedFlag definedFlag = new DefinedFlag(blade,
                    parameterName,
                    type,
                    parameter,
                    flag);

                this.flagProviders.add(provider);
                this.parameters.add(definedFlag);
            } else {
                DefinedArgument definedArgument = new DefinedArgument(blade,
                    parameterName,
                    type,
                    parameterData == null
                        ? Collections.emptyList()
                        : Arrays.asList(parameterData),
                    parameter);

                definedArgument.alwaysQuoted = provider != null && provider.alwaysParseQuotes();

                this.argumentProviders.add(provider);
                this.parameters.add(definedArgument);
            }

            i++;
        }
    }

    /**
     * Gets or loads the usage message for this command.
     *
     * @return the usage message
     */
    @NotNull
    public CommandFeedback<?> usageMessage() {
        return usageMessage.ensureGetOrLoad(
            () -> blade.configuration().feedbackCreator().create(this, true));
    }

    /**
     * Gets or loads the help message for this command.
     *
     * @return the help message
     */
    @NotNull
    public CommandFeedback<?> helpMessage() {
        return helpMessage.ensureGetOrLoad(
            () -> blade.configuration().feedbackCreator().create(this, false));
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
    @SuppressWarnings("unused")
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

    @ApiStatus.Internal
    private AnnotatedElement createHelpGreedyArgument() {
        return new AnnotatedElement() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
                if (annotationClass == Greedy.class)
                    return (T) new Greedy() {
                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return Greedy.class;
                        }
                    };

                return null;
            }

            @Override
            public @NotNull Annotation @NotNull [] getAnnotations() {
                Greedy greedy = Objects.requireNonNull(getAnnotation(Greedy.class));
                return new Annotation[]{ greedy };
            }

            @Override
            public @NotNull Annotation @NotNull [] getDeclaredAnnotations() {
                Greedy greedy = Objects.requireNonNull(getAnnotation(Greedy.class));
                return new Annotation[]{ greedy };
            }
        };
    }

}
