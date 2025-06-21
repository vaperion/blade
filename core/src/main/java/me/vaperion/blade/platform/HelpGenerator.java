package me.vaperion.blade.platform;

import me.vaperion.blade.command.Command;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@FunctionalInterface
public interface HelpGenerator {
    @NotNull
    List<String> generate(@NotNull Context context, @NotNull List<Command> commands);

    @SuppressWarnings("unused")
    final class Default implements HelpGenerator {
        @NotNull
        @Override
        public List<String> generate(@NotNull Context context, @NotNull List<Command> commands) {
            throw new BladeExitMessage("This command failed to execute as we couldn't find its registration.");
        }
    }
}