package me.vaperion.blade.container;

import me.vaperion.blade.Blade;
import me.vaperion.blade.command.Command;
import org.jetbrains.annotations.NotNull;

public interface ContainerCreator<T extends Container> {
    @NotNull
    T create(@NotNull Blade blade, @NotNull Command command, @NotNull String alias) throws Exception;
}
