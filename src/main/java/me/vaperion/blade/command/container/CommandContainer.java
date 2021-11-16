package me.vaperion.blade.command.container;

import me.vaperion.blade.command.command.BladeCommand;
import me.vaperion.blade.command.service.BladeCommandService;
import org.jetbrains.annotations.NotNull;

public interface CommandContainer {
    @NotNull
    BladeCommandService getCommandService();

    @NotNull
    BladeCommand getParentCommand();
}
