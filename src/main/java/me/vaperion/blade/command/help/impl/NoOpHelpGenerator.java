package me.vaperion.blade.command.help.impl;

import me.vaperion.blade.command.command.BladeCommand;
import me.vaperion.blade.command.context.BladeContext;
import me.vaperion.blade.command.exception.BladeExitMessage;
import me.vaperion.blade.command.help.HelpGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NoOpHelpGenerator implements HelpGenerator {

    @NotNull
    @Override
    public List<String> generate(@NotNull BladeContext context, @NotNull List<BladeCommand> commands) {
        throw new BladeExitMessage("This command failed to execute as we couldn't find its registration.");
    }
}
