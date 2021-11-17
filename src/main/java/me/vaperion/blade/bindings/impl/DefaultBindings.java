package me.vaperion.blade.bindings.impl;

import com.google.common.collect.ImmutableMap;
import me.vaperion.blade.annotation.Range;
import me.vaperion.blade.bindings.Binding;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.service.BladeCommandService;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DefaultBindings implements Binding {

    private static final Map<String, Boolean> BOOLEAN_MAP = ImmutableMap.<String, Boolean>builder()
            .put("true", true).put("1", true).put("yes", true)
            .put("false", false).put("0", false).put("no", false)
            .build();

    @Override
    public void bind(@NotNull BladeCommandService commandService) {
        commandService.bindProvider(UUID.class, (ctx, arg) -> {
            try {
                return UUID.fromString(arg.getString());
            } catch (Exception ex) {
                if (arg.getParameter().ignoreFailedArgumentParse()) return null;
                throw new BladeExitMessage("Error: '" + arg.getString() + "' is not a valid UUID.");
            }
        });

        commandService.bindProvider(String.class, (ctx, arg) -> {
            if (arg.getString() == null) {
                if (arg.getParameter().ignoreFailedArgumentParse()) return null;
                throw new BladeExitMessage("Error: '" + arg.getString() + "' is not a valid string.");
            }

            return arg.getString();
        });

        commandService.bindProvider(boolean.class, (ctx, arg) -> {
            Boolean bool = BOOLEAN_MAP.get(arg.getString().toLowerCase(Locale.ROOT));

            if (bool == null) {
                if (arg.getParameter().ignoreFailedArgumentParse()) return null;
                throw new BladeExitMessage("Error: '" + arg.getString() + "' is not a valid logical value.");
            }

            return bool;
        });

        commandService.bindProvider(int.class, (ctx, arg) -> {
            int input;
            try {
                input = Integer.parseInt(arg.getString());
            } catch (NumberFormatException e) {
                throw new BladeExitMessage("Error: Invalid number: " + arg.getString());
            }

            if (arg.getParameter().hasRange()) {
                Range range = arg.getParameter().getRange();

                if (!Double.isNaN(range.min()) && input < range.min())
                    throw new BladeExitMessage("Error: The provided number '" + input + "' must be at least " + range.min() + ".");
                else if (!Double.isNaN(range.max()) && input > range.max())
                    throw new BladeExitMessage("Error: The provided number '" + input + "' must be at most " + range.max() + ".");
            }

            return input;
        });

        commandService.bindProvider(long.class, (ctx, arg) -> {
            long input;
            try {
                input = Long.parseLong(arg.getString());
            } catch (NumberFormatException e) {
                throw new BladeExitMessage("Error: Invalid number: " + arg.getString());
            }

            if (arg.getParameter().hasRange()) {
                Range range = arg.getParameter().getRange();

                if (!Double.isNaN(range.min()) && input < range.min())
                    throw new BladeExitMessage("Error: The provided number '" + input + "' must be at least " + range.min() + ".");
                else if (!Double.isNaN(range.max()) && input > range.max())
                    throw new BladeExitMessage("Error: The provided number '" + input + "' must be at most " + range.max() + ".");
            }

            return input;
        });

        commandService.bindProvider(double.class, (ctx, arg) -> {
            double input;
            try {
                input = Double.parseDouble(arg.getString());
            } catch (NumberFormatException e) {
                throw new BladeExitMessage("Error: Invalid number: " + arg.getString());
            }

            if (arg.getParameter().hasRange()) {
                Range range = arg.getParameter().getRange();

                if (!Double.isNaN(range.min()) && input < range.min())
                    throw new BladeExitMessage("Error: The provided number '" + input + "' must be at least " + range.min() + ".");
                else if (!Double.isNaN(range.max()) && input > range.max())
                    throw new BladeExitMessage("Error: The provided number '" + input + "' must be at most " + range.max() + ".");
            }

            return input;
        });
    }
}
