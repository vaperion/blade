package me.vaperion.blade.argument.impl;

import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class BooleanArgument implements ArgumentProvider<Boolean> {
    private static final Map<String, Boolean> BOOLEAN_MAP = new HashMap<>();

    static {
        BOOLEAN_MAP.put("true", true);
        BOOLEAN_MAP.put("false", false);
        BOOLEAN_MAP.put("yes", true);
        BOOLEAN_MAP.put("no", false);
        BOOLEAN_MAP.put("on", true);
        BOOLEAN_MAP.put("off", false);
    }

    @Override
    public @Nullable Boolean provide(@NotNull Context ctx, @NotNull InputArgument arg) throws BladeParseError {
        Boolean bool = BOOLEAN_MAP.get(arg.requireValue().toLowerCase(Locale.ROOT));

        if (bool == null) {
            String truthy = BOOLEAN_MAP.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));

            String falsy = BOOLEAN_MAP.entrySet().stream()
                .filter(e -> !e.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));

            throw BladeParseError.recoverable(String.format(
                "'%s' is not a valid boolean. Use: %s / %s",
                arg.value(),
                truthy,
                falsy
            ));
        }

        return bool;
    }

    @Override
    public void suggest(@NotNull Context ctx,
                        @NotNull InputArgument arg,
                        @NotNull SuggestionsBuilder suggestions) throws BladeParseError {
        // Only suggest true/false even though we accept more values

        String input = arg.requireValue().toLowerCase(Locale.ROOT);

        if ("true" .startsWith(input))
            suggestions.suggest("true");

        if ("false" .startsWith(input))
            suggestions.suggest("false");
    }

    @Override
    public @Nullable String defaultArgName(@NotNull AnnotatedElement element) {
        return "boolean";
    }
}
