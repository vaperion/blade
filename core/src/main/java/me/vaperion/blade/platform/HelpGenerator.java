package me.vaperion.blade.platform;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.internal.BladeFatalError;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@FunctionalInterface
public interface HelpGenerator<Text> {
    @NotNull
    List<Text> generate(@NotNull Context context, @NotNull List<BladeCommand> commands);

    @SuppressWarnings("unused")
    final class Default<T> implements HelpGenerator<T> {
        @NotNull
        @Override
        public List<T> generate(@NotNull Context context, @NotNull List<BladeCommand> commands) {
            throw new BladeFatalError("This command failed to execute as we couldn't find its registration.");
        }
    }
}