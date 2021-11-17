package me.vaperion.blade.container;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.service.BladeCommandService;
import org.jetbrains.annotations.NotNull;

public interface CommandContainer {
    @NotNull
    BladeCommandService getCommandService();

    @NotNull
    BladeCommand getParentCommand();
}
