package me.vaperion.blade.container;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.service.BladeCommandService;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ContainerCreator<T extends CommandContainer> {
    ContainerCreator<?> NONE = (service, command, alias, fallbackPrefix) -> {
        throw new UnsupportedOperationException("Not implemented");
    };

    @NotNull
    T create(@NotNull BladeCommandService commandService, @NotNull BladeCommand bladeCommand, @NotNull String realAlias, @NotNull String fallbackPrefix) throws Exception;
}
