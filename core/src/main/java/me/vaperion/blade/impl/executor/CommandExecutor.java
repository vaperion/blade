package me.vaperion.blade.impl.executor;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.parameter.Opt;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.BladeParameter;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.exception.BladeUsageMessage;
import me.vaperion.blade.exception.internal.BladeFatalError;
import me.vaperion.blade.exception.internal.BladeImplementationError;
import me.vaperion.blade.exception.internal.BladeInternalError;
import me.vaperion.blade.exception.internal.BladeInvocationError;
import me.vaperion.blade.impl.node.ResolvedCommand;
import me.vaperion.blade.sender.internal.SndProvider;
import me.vaperion.blade.tokenizer.TokenizerError;
import me.vaperion.blade.tokenizer.input.CommandInput;
import me.vaperion.blade.tokenizer.input.InputOption;
import me.vaperion.blade.tokenizer.input.token.flag.FlagValue;
import me.vaperion.blade.tokenizer.input.token.impl.ArgumentToken;
import me.vaperion.blade.tokenizer.input.token.impl.FlagToken;
import me.vaperion.blade.util.ErrorMessage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static me.vaperion.blade.util.BladeHelper.ERROR_MESSAGE;

@RequiredArgsConstructor
public final class CommandExecutor {

    private final Blade blade;

    @Nullable
    public ErrorMessage execute(@NotNull Context context,
                                @NotNull CommandInput input,
                                @NotNull ResolvedCommand node) {
        return executeSafely(context, node, () -> prepareAndInvoke(context, input, node));
    }

    @Nullable
    public ErrorMessage execute(@NotNull Context context,
                                @NotNull ResolvedCommand node,
                                @NotNull String input) {
        return execute(context, node, input, EnumSet.noneOf(InputOption.class));
    }

    @Nullable
    public ErrorMessage execute(@NotNull Context context,
                                @NotNull ResolvedCommand node,
                                @NotNull String input,
                                @NotNull EnumSet<InputOption> options) {
        return executeSafely(context, node, () -> prepareAndInvoke(context, input, node, options));
    }

    @Nullable
    private ErrorMessage executeSafely(@NotNull Context context,
                                       @NotNull ResolvedCommand node,
                                       @NotNull Runnable runnable) {
        if (node.command() != null && Objects.requireNonNull(node.command()).helpCommand()) {
            // Help commands are handled by the container
            return ErrorMessage.showCommandHelp();
        }

        try {
            runnable.run();

            return null;
        } catch (BladeParseError e) {
            // Rethrow parse errors to be handled by the caller
            throw e;
        } catch (TargetedUsageMessage e) {
            return ErrorMessage.showCommandUsage(e.command);
        } catch (BladeUsageMessage ignored) {
            return ErrorMessage.showCommandUsage();
        } catch (BladeFatalError e) {
            return ErrorMessage.lines(e.getMessage());
        } catch (BladeImplementationError e) {
            blade.logger().error(e,
                "An error occurred while invoking command '%s' for sender %s. This is a bug in your plugin.",
                context.label(),
                context.sender().name()
            );

            return ErrorMessage.lines(ERROR_MESSAGE);
        } catch (BladeInternalError e) {
            blade.logger().error(e,
                "An error occurred while invoking command '%s' for sender %s. This is a bug in Blade, not your plugin. Please report it.",
                context.label(),
                context.sender().name()
            );

            return ErrorMessage.lines(ERROR_MESSAGE);
        } catch (BladeInvocationError e) {
            blade.logger().error(e,
                "An error occurred while invoking command '%s' for sender %s",
                context.label(),
                context.sender().name()
            );

            return ErrorMessage.lines(ERROR_MESSAGE);
        } catch (Throwable t) {
            blade.logger().error(t,
                "An unexpected error occurred while invoking command '%s' for sender %s",
                context.label(),
                context.sender().name()
            );

            return ErrorMessage.lines(ERROR_MESSAGE);
        }
    }

    private void prepareAndInvoke(@NotNull Context context,
                                  @NotNull CommandInput input,
                                  @NotNull ResolvedCommand node) {
        List<BladeCommand> overloads = node.overloads();

        if (overloads.size() > 1) {
            invokeBestOverload(context, input, node, overloads);
            return;
        }

        invokeSingle(context, input, node);
    }

    private void prepareAndInvoke(@NotNull Context context,
                                  @NotNull String rawInput,
                                  @NotNull ResolvedCommand node,
                                  @NotNull EnumSet<InputOption> options) {
        List<BladeCommand> overloads = node.overloads();

        if (overloads.size() > 1) {
            invokeBestOverload(context, rawInput, options, node, overloads);
            return;
        }

        BladeCommand cmd = node.command();

        if (cmd == null) {
            throw new BladeInternalError(String.format(
                "Resolved command node for command '%s' does not have a command associated with it!",
                context.label()
            ));
        }

        if (cmd.usesBladeContext()) {
            invoke0(cmd, new Object[]{ context });
            return;
        }

        CommandInput input = inputForCommand(cmd, rawInput, options, node.matchedLabelOr(context.label()));
        context.updateArgumentsFromInput(input);
        invoke0(cmd, bind(context, input, cmd, false));
    }

    private void invokeSingle(@NotNull Context context,
                              @NotNull CommandInput input,
                              @NotNull ResolvedCommand node) {
        BladeCommand cmd = node.command();

        if (cmd == null) {
            throw new BladeInternalError(String.format(
                "Resolved command node for command '%s' does not have a command associated with it!",
                context.label()
            ));
        }

        if (cmd.usesBladeContext()) {
            invoke0(cmd, new Object[]{ context });
            return;
        }

        invoke0(cmd, bind(context, input, cmd, false));
    }

    private void invokeBestOverload(@NotNull Context context,
                                    @NotNull CommandInput input,
                                    @NotNull ResolvedCommand node,
                                    @NotNull List<BladeCommand> overloads) {
        String matchedLabel = node.matchedLabelOr(context.label());
        invokeBestOverload(context, overloads,
            cmd -> inputForOverload(input, cmd, matchedLabel));
    }

    private void invokeBestOverload(@NotNull Context context,
                                    @NotNull String rawInput,
                                    @NotNull EnumSet<InputOption> options,
                                    @NotNull ResolvedCommand node,
                                    @NotNull List<BladeCommand> overloads) {
        String matchedLabel = node.matchedLabelOr(context.label());
        invokeBestOverload(context, overloads,
            cmd -> inputForCommand(cmd, rawInput, options, matchedLabel));
    }

    private void invokeBestOverload(@NotNull Context context,
                                    @NotNull List<BladeCommand> overloads,
                                    @NotNull OverloadInputFactory inputFactory) {
        List<BladeCommand> candidates = new ArrayList<>(overloads);
        candidates.sort(Comparator
            .comparing(BladeCommand::usesBladeContext)
            .thenComparing(Comparator.comparingInt(CommandExecutor::requiredArgumentCount).reversed())
            .thenComparingInt(c -> c.arguments().size()));

        RuntimeException bestError = null;
        BladeCommand bestUsageOverload = null;
        int bestScore = -1;
        RuntimeException tokenizationError = null;
        BladeCommand lenientFallback = null;
        CommandInput lenientFallbackInput = null;

        for (BladeCommand cmd : candidates) {
            if (cmd.helpCommand()) {
                // Help commands are handled by the container
                continue;
            }

            if (!cmd.hasPermission(context)) {
                continue;
            }

            if (cmd.usesBladeContext()) {
                // Context-based commands accept any input
                invoke0(cmd, new Object[]{ context });
                return;
            }

            CommandInput cmdInput = null;
            Object[] args;

            try {
                cmdInput = inputFactory.create(cmd);
                args = bind(context, cmdInput, cmd, true);
            } catch (BladeUsageMessage e) {
                // Try the next overload
                continue;
            } catch (TrialExcessArguments e) {
                if (!blade.configuration().strictArgumentCount()) {
                    if (lenientFallback == null) {
                        lenientFallback = cmd;
                        lenientFallbackInput = cmdInput;
                    }
                    continue;
                }

                int score = e.bound * 2 + 1;

                if (score > bestScore) {
                    bestScore = score;
                    bestUsageOverload = null;
                    bestError = BladeParseError.fatal(String.format(
                        "Too many arguments. Please remove the last %d argument%s.",
                        e.extra,
                        e.extra == 1 ? "" : "s"
                    ));
                }
                continue;
            } catch (TrialMissingArguments e) {
                int score = e.bound * 2;

                if (score > bestScore) {
                    bestScore = score;
                    bestError = null;
                    bestUsageOverload = cmd;
                }
                continue;
            } catch (TrialParseFailure e) {
                int score = e.depth * 2 + (e.arityMatches ? 1 : 0);

                if (score > bestScore) {
                    bestScore = score;
                    bestUsageOverload = null;
                    bestError = e.error;
                }
                continue;
            } catch (BladeParseError | TokenizerError e) {
                if (tokenizationError == null) {
                    tokenizationError = e;
                }
                continue;
            }

            context.updateArgumentsFromInput(cmdInput);
            invoke0(cmd, args);
            return;
        }

        if (lenientFallback != null) {
            Object[] args = bind(context, lenientFallbackInput, lenientFallback, false);

            context.updateArgumentsFromInput(lenientFallbackInput);
            invoke0(lenientFallback, args);
            return;
        }

        if (bestError != null) {
            throw bestError;
        }

        if (bestUsageOverload != null) {
            throw new TargetedUsageMessage(bestUsageOverload);
        }

        if (tokenizationError != null) {
            throw tokenizationError;
        }

        throw new BladeUsageMessage();
    }

    private interface OverloadInputFactory {
        @NotNull CommandInput create(@NotNull BladeCommand command);
    }

    private static final class TrialParseFailure extends RuntimeException {
        private final BladeParseError error;
        private final int depth;
        private final boolean arityMatches;

        private TrialParseFailure(BladeParseError error, int depth, boolean arityMatches) {
            super(null, null, false, false);
            this.error = error;
            this.depth = depth;
            this.arityMatches = arityMatches;
        }
    }

    private static final class TrialMissingArguments extends RuntimeException {
        private final int bound;

        private TrialMissingArguments(int bound) {
            super(null, null, false, false);
            this.bound = bound;
        }
    }

    private static final class TargetedUsageMessage extends BladeUsageMessage {
        private final BladeCommand command;

        private TargetedUsageMessage(BladeCommand command) {
            this.command = command;
        }
    }

    private static final class TrialExcessArguments extends RuntimeException {
        private final int extra;
        private final int bound;

        private TrialExcessArguments(int extra, int bound) {
            super(null, null, false, false);
            this.extra = extra;
            this.bound = bound;
        }
    }

    @NotNull
    private CommandInput inputForOverload(@NotNull CommandInput original,
                                          @NotNull BladeCommand cmd,
                                          @NotNull String matchedLabel) {
        if (original.bladeCommand() == cmd) {
            // The container already tokenized the input
            return original;
        }

        return inputForCommand(cmd, original.input(), original.options(), matchedLabel);
    }

    @NotNull
    private CommandInput inputForCommand(@NotNull BladeCommand cmd,
                                         @NotNull String rawInput,
                                         @NotNull EnumSet<InputOption> options,
                                         @NotNull String matchedLabel) {
        CommandInput input = new CommandInput(blade, cmd, rawInput, options);
        input.useWholeLabel(matchedLabel);
        input.tokenize();

        if (!input.mergeTokensToFormWholeLabel(matchedLabel)) {
            throw new BladeUsageMessage();
        }

        return input;
    }

    private static int requiredArgumentCount(@NotNull BladeCommand cmd) {
        int count = 0;

        for (DefinedArgument argument : cmd.arguments()) {
            if (!argument.isOptional()) {
                count++;
            }
        }

        return count;
    }

    @NotNull
    private Object[] bind(@NotNull Context context,
                          @NotNull CommandInput input,
                          @NotNull BladeCommand cmd,
                          boolean exactArgumentCount) {
        List<Object> methodArgs = new ArrayList<>();

        List<ArgumentToken> argumentTokens = input.arguments();
        Map<Character, FlagValue> mergedFlags = mergeAllFlags(input.flags());

        boolean trialArityMatches = argumentTokens.size() >= requiredArgumentCount(cmd);

        int argIndex = 0, flagIndex = 0;
        boolean skipLengthEnforcement = false;

        for (BladeParameter parameter : cmd.parameters()) {
            if (parameter instanceof DefinedArgument) {
                String value;
                Opt optional = null;
                boolean wasProvided = false;

                if (argumentTokens.size() <= argIndex) {
                    // No more argument tokens available

                    if (!parameter.isOptional()) {
                        // Required parameter missing

                        if (exactArgumentCount && !argumentTokens.isEmpty()) {
                            throw new TrialMissingArguments(argumentTokens.size());
                        }

                        throw new BladeUsageMessage();
                    } else {
                        optional = parameter.optional();
                    }

                    value = getDefaultValue(parameter);
                } else {
                    if (parameter.type() == String.class && parameter.isGreedy()) {
                        // Must be the last argument
                        if (argIndex != cmd.argumentProviders().size() - 1) {
                            throw new BladeFatalError(String.format(
                                "Greedy argument parameter '%s' of command '%s' must be the last argument.",
                                parameter.name(),
                                context.label()
                            ));
                        }

                        value = argumentTokens
                            .subList(argIndex, argumentTokens.size())
                            .stream()
                            .map(ArgumentToken::value)
                            .collect(Collectors.joining(" "));

                        skipLengthEnforcement = true;
                    } else {
                        value = argumentTokens.get(argIndex).value();
                    }

                    wasProvided = true;
                }

                ArgumentProvider<?> provider = parameter.hasCustomParser()
                    ? parameter.customParser()
                    : cmd.argumentProviders().get(argIndex);

                if (provider == null) {
                    throw new BladeImplementationError(String.format(
                        "No argument provider found for parameter '%s' of command '%s'!",
                        parameter.name(),
                        context.label()
                    ));
                }

                int parseDepth = argIndex;

                Object methodArg;

                boolean isSenderType = optional != null && optional.value() == Opt.Type.SENDER;

                if (value == null && !isSenderType && !provider.handlesNullInputArguments()) {
                    if (parameter.type().isPrimitive()) {
                        throw new BladeInternalError(String.format(
                            "Parameter '%s' of command '%s' is primitive but default value is null!",
                            parameter.name(),
                            context.label()
                        ));
                    }

                    methodArg = null;
                } else {
                    InputArgument inputArg = new InputArgument(
                        parameter,
                        value,
                        wasProvided
                            ? InputArgument.Status.PRESENT
                            : InputArgument.Status.NOT_PRESENT
                    );

                    inputArg.data().addAll(parameter.data());
                    inputArg.addAnnotations(parameter.annotations());

                    try {
                        methodArg = provideArgument(
                            provider,
                            context,
                            inputArg,
                            optional != null && optional.treatErrorAsEmpty(),
                            optional != null
                        );
                    } catch (BladeParseError e) {
                        if (exactArgumentCount) {
                            throw new TrialParseFailure(e, parseDepth, trialArityMatches);
                        }

                        throw e;
                    }
                }

                methodArgs.add(methodArg);

                argIndex++;
            } else if (parameter instanceof DefinedFlag) {
                DefinedFlag flag = (DefinedFlag) parameter;

                String value;
                Opt optional = null;
                boolean wasProvided = false;

                if (mergedFlags.containsKey(flag.getChar())) {
                    FlagValue flagValue = mergedFlags.get(flag.getChar());

                    value = flagValue.value();
                    wasProvided = true;
                } else if (!flag.isBooleanFlag()) {
                    if (!parameter.isOptional()) {
                        // Required flag missing
                        throw new BladeUsageMessage();
                    } else {
                        optional = parameter.optional();
                    }

                    value = getDefaultValue(parameter);
                } else {
                    // Boolean flags are always optional and default to false if not provided
                    value = "false";
                }

                ArgumentProvider<?> provider = flag.hasCustomParser()
                    ? flag.customParser()
                    : cmd.flagProviders().get(flagIndex);

                if (provider == null) {
                    throw new BladeInternalError(String.format(
                        "No flag provider found for parameter '%s' of command '%s'!",
                        parameter.name(),
                        context.label()
                    ));
                }

                int parseDepth = argIndex;

                Object methodArg;

                boolean isSenderType = optional != null && optional.value() == Opt.Type.SENDER;

                if (value == null && !isSenderType && !provider.handlesNullInputArguments()) {
                    if (parameter.type().isPrimitive()) {
                        throw new BladeInternalError(String.format(
                            "Parameter '%s' of command '%s' is primitive but default value is null!",
                            parameter.name(),
                            context.label()
                        ));
                    }

                    methodArg = null;
                } else {
                    InputArgument inputArg = new InputArgument(
                        parameter,
                        value,
                        wasProvided
                            ? InputArgument.Status.PRESENT
                            : InputArgument.Status.NOT_PRESENT
                    );

                    inputArg.data().addAll(parameter.data());
                    inputArg.addAnnotations(parameter.annotations());

                    try {
                        methodArg = provideArgument(
                            provider,
                            context,
                            inputArg,
                            optional != null && optional.treatErrorAsEmpty(),
                            optional != null
                        );
                    } catch (BladeParseError e) {
                        if (exactArgumentCount) {
                            throw new TrialParseFailure(e, parseDepth, trialArityMatches);
                        }

                        throw e;
                    }
                }

                methodArgs.add(methodArg);

                flagIndex++;
            } else {
                throw new BladeInternalError(String.format(
                    "Unhandled internal parameter type '%s' for parameter '%s' of command '%s'!",
                    parameter.getClass().getName(),
                    parameter.name(),
                    context.label()
                ));
            }
        }

        if (!skipLengthEnforcement && argIndex < argumentTokens.size()) {
            if (exactArgumentCount) {
                throw new TrialExcessArguments(argumentTokens.size() - argIndex, argIndex);
            }

            if (blade.configuration().strictArgumentCount()) {
                // Argument count must match exactly
                int extra = argumentTokens.size() - argIndex;

                throw BladeParseError.fatal(String.format(
                    "Too many arguments. Please remove the last %d argument%s.",
                    extra,
                    extra == 1 ? "" : "s"
                ));
            }
        }

        boolean prependSender = cmd.hasSenderParameter() || cmd.usesBladeSender();
        Object[] finalArgs = new Object[methodArgs.size() + (prependSender ? 1 : 0)];

        if (prependSender) {
            if (cmd.usesBladeSender()) {
                finalArgs[0] = context.sender();
            } else {
                try {
                    finalArgs[0] = adaptSender(cmd, context);
                } catch (BladeParseError e) {
                    if (exactArgumentCount) {
                        throw new TrialParseFailure(e, argumentTokens.size() + 1, true);
                    }

                    throw e;
                }
            }
        }

        System.arraycopy(
            methodArgs.toArray(),
            0,
            finalArgs,
            prependSender ? 1 : 0,
            methodArgs.size()
        );

        return finalArgs;
    }

    private void invoke0(@NotNull BladeCommand cmd,
                         @NotNull Object[] args) {
        try {
            cmd.method().invoke(
                cmd.instance(),
                args
            );
        } catch (InvocationTargetException e) {
            throw invocationFailure(cmd, args, e.getTargetException());
        } catch (Throwable t) {
            throw invocationFailure(cmd, args, t);
        }
    }

    @NotNull
    private RuntimeException invocationFailure(@NotNull BladeCommand cmd,
                                               @NotNull Object[] args,
                                               @NotNull Throwable cause) {
        if (cause instanceof BladeFatalError ||
            cause instanceof BladeImplementationError ||
            cause instanceof BladeInternalError ||
            cause instanceof BladeInvocationError ||
            cause instanceof BladeParseError ||
            cause instanceof BladeUsageMessage) {
            return (RuntimeException) cause;
        }

        return new BladeInvocationError(
            String.format(
                "Command invocation failed (method: %s.%s, args: %s)",
                cmd.method().getDeclaringClass().getName(),
                cmd.method().getName(),
                Arrays.toString(args)
            ),
            cause);
    }

    @ApiStatus.Internal
    @NotNull
    public Object adaptSender(@NotNull BladeCommand cmd,
                              @NotNull Context context) {
        Object sender = null;
        String friendlyName = null;

        for (SndProvider<?> provider : cmd.senderProviders()) {
            Object result = provider.provider().provide(context, context.sender());
            friendlyName = provider.provider().friendlyName(true);

            if (result != null) {
                // Successfully adapted

                sender = result;
                break;
            }
        }

        if (sender == null && context.sender().isExpectedType(cmd)) {
            // Sender is already of the expected type

            sender = context.sender().parseAs(cmd.senderType());

            if (sender == null) {
                // This shouldn't happen, but we'll fall back to the raw underlying sender just in case

                sender = context.sender().underlyingSender();
            }
        }

        if (sender != null && !cmd.senderType().isAssignableFrom(sender.getClass())) {
            // Sanity check. This shouldn't really happen unless one of the sender providers misbehaved.

            sender = null;
        }

        if (friendlyName == null)
            friendlyName = blade.platform().convertSenderTypeToName(
                cmd.senderType(),
                true
            );

        if (sender == null) {
            throw BladeParseError.fatal(String.format(
                "This command can only be executed by %s.",
                friendlyName
            ));
        }

        return sender;
    }

    @ApiStatus.Internal
    @Nullable
    public Object provideArgument(@NotNull ArgumentProvider<?> provider,
                                  @NotNull Context context,
                                  @NotNull InputArgument inputArg,
                                  boolean ignoreErrors,
                                  boolean tryRecover) {
        boolean retry = false;
        Throwable throwable = null;

        try {
            return provider.provide(context, inputArg);
        } catch (BladeParseError e) {
            if ((ignoreErrors || e.isRecoverable()) && tryRecover) {
                retry = true;
            } else {
                // Rethrow this parse error as-is so it can be handled by the caller
                throw e;
            }
        } catch (Throwable t) {
            if (ignoreErrors) {
                retry = true;
            } else {
                // Wrap and rethrow other exceptions as invocation errors
                throwable = t;
            }
        }

        if (retry) {
            inputArg.value(
                getOptionalEmptyValue(inputArg.parameter().type())
            );

            inputArg.status(InputArgument.Status.NOT_PRESENT);

            return provideArgument(
                provider,
                context,
                inputArg,
                false,
                false
            );
        }

        throw new BladeInvocationError(String.format(
            "Failed to provide argument for parameter '%s' of type '%s' using provider '%s'.",
            inputArg.parameter().name(),
            inputArg.parameter().type().getName(),
            provider.getClass().getName()
        ), throwable);
    }

    @ApiStatus.Internal
    @Nullable
    public String getDefaultValue(@NotNull BladeParameter parameter) {
        if (!parameter.isOptional())
            return null;

        Opt optional = parameter.optional();
        assert optional != null;

        switch (optional.value()) {
            case EMPTY_OR_CUSTOM:
                if (!optional.custom().isEmpty()) {
                    return optional.custom();
                }

                Class<?> paramType = parameter.type();
                return getOptionalEmptyValue(paramType);

            case EMPTY:
                Class<?> type = parameter.type();
                return getOptionalEmptyValue(type);

            case SENDER:
                return null;

            case CUSTOM:
                return optional.custom();
        }

        throw new UnsupportedOperationException("Unhandled optional value: " + optional.value());
    }

    @Nullable
    private String getOptionalEmptyValue(@NotNull Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }

        if (type == boolean.class || type == Boolean.class)
            return "false";
        else if (type == int.class || type == Integer.class)
            return "0";
        else if (type == long.class || type == Long.class)
            return "0";
        else if (type == short.class || type == Short.class)
            return "0";
        else if (type == byte.class || type == Byte.class)
            return "0";
        else if (type == double.class || type == Double.class)
            return "0.0";
        else if (type == float.class || type == Float.class)
            return "0.0";
        else if (type == char.class || type == Character.class)
            return "\u0000";

        throw new UnsupportedOperationException("Unsupported primitive type: " + type.getName());
    }

    @NotNull
    private Map<Character, FlagValue> mergeAllFlags(@NotNull List<FlagToken> tokens) {
        Map<Character, FlagValue> mergedFlags = new HashMap<>();

        for (FlagToken token : tokens) {
            token.flags().forEach((ch, value) -> {
                if (blade.configuration().strictFlagCount() && mergedFlags.containsKey(ch)) {
                    throw BladeParseError.fatal(String.format(
                        "Flag '-%c' was provided multiple times. Please remove one of the instances.",
                        ch
                    ));
                } else {
                    mergedFlags.put(ch, value);
                }
            });
        }

        return mergedFlags;
    }
}
