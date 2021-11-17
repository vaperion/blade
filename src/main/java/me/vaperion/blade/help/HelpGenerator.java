package me.vaperion.blade.help;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.BladeContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@FunctionalInterface
public interface HelpGenerator {
    @NotNull
    List<String> generate(@NotNull BladeContext context, @NotNull List<BladeCommand> commands);
}
