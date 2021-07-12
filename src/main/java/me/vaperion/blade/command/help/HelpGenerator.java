package me.vaperion.blade.command.help;

import me.vaperion.blade.command.container.BladeCommand;
import me.vaperion.blade.command.context.BladeContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@FunctionalInterface
public interface HelpGenerator {
    @NotNull
    List<String> generate(@NotNull BladeContext context, @NotNull List<BladeCommand> commands);
}
