package me.vaperion.blade.help.impl;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.help.HelpGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NoOpHelpGenerator implements HelpGenerator {

    @NotNull
    @Override
    public List<String> generate(@NotNull BladeContext context, @NotNull List<BladeCommand> commands) {
        throw new BladeExitMessage("This command failed to execute as we couldn't find its registration.");
    }
}
