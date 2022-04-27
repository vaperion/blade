package me.vaperion.blade.argument.impl;

import me.vaperion.blade.annotation.Range;
import me.vaperion.blade.argument.Argument;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

public class IntArgument implements ArgumentProvider<Integer> {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.#");

    @Override
    public @Nullable Integer provide(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        int input;
        try {
            input = Integer.parseInt(arg.getString());
        } catch (NumberFormatException e) {
            throw new BladeExitMessage("Error: '" + arg.getString() + "' is not a valid number.");
        }

        if (arg.getParameter().hasRange()) {
            Range range = arg.getParameter().getRange();

            if (!Double.isNaN(range.min()) && input < range.min())
                throw new BladeExitMessage("Error: The provided number '" + input + "' must be at least " + NUMBER_FORMAT.format(range.min()) + ".");
            else if (!Double.isNaN(range.max()) && input > range.max())
                throw new BladeExitMessage("Error: The provided number '" + input + "' must be at most " + NUMBER_FORMAT.format(range.max()) + ".");
        }

        return input;
    }
}
