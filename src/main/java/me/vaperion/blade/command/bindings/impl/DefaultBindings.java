package me.vaperion.blade.command.bindings.impl;

import me.vaperion.blade.command.annotation.Range;
import me.vaperion.blade.command.bindings.Binding;
import me.vaperion.blade.command.exception.BladeExitMessage;
import me.vaperion.blade.command.service.BladeCommandService;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DefaultBindings implements Binding {

    @Override
    public void bind(@NotNull BladeCommandService commandService) {
        commandService.bindProvider(UUID.class, (ctx, param, input) -> {
            if (input == null) return null;
            try {
                return UUID.fromString(input);
            } catch (Exception ex) {
                throw new BladeExitMessage("Invalid UUID: '" + input + "'");
            }
        });

        commandService.bindProvider(String.class, (ctx, param, input) -> input == null && param.isOptional() ? param.getDefault() : input);
        commandService.bindProvider(boolean.class, (ctx, param, input) -> input != null && (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("1")));

        commandService.bindProvider(int.class, (ctx, param, inputStr) -> {
            inputStr = inputStr == null && param.isOptional() ? param.getDefault() : inputStr;
            if (inputStr == null) return null;
            int input = Integer.parseInt(inputStr);

            if (param.hasRange()) {
                Range range = param.getRange();

                if (!Double.isNaN(range.min()) && input < range.min())
                    throw new BladeExitMessage("The provided input value is too low.");
                else if (!Double.isNaN(range.max()) && input > range.max())
                    throw new BladeExitMessage("The provided input value is too high.");
            }

            return input;
        });

        commandService.bindProvider(long.class, (ctx, param, inputStr) -> {
            inputStr = inputStr == null && param.isOptional() ? param.getDefault() : inputStr;
            if (inputStr == null) return null;
            long input = Long.parseLong(inputStr);

            if (param.hasRange()) {
                Range range = param.getRange();

                if (!Double.isNaN(range.min()) && input < range.min())
                    throw new BladeExitMessage("The provided input value is too low.");
                else if (!Double.isNaN(range.max()) && input > range.max())
                    throw new BladeExitMessage("The provided input value is too high.");
            }

            return input;
        });

        commandService.bindProvider(double.class, (ctx, param, inputStr) -> {
            inputStr = inputStr == null && param.isOptional() ? param.getDefault() : inputStr;
            if (inputStr == null) return null;
            double input = Double.parseDouble(inputStr);

            if (param.hasRange()) {
                Range range = param.getRange();

                if (!Double.isNaN(range.min()) && input < range.min())
                    throw new BladeExitMessage("The provided input value is too low.");
                else if (!Double.isNaN(range.max()) && input > range.max())
                    throw new BladeExitMessage("The provided input value is too high.");
            }

            return input;
        });
    }
}
