package me.vaperion.blade.container;

import me.vaperion.blade.Blade;
import me.vaperion.blade.command.Command;
import org.jetbrains.annotations.NotNull;

public interface Container {
    @NotNull Blade getBlade();

    @NotNull Command getBaseCommand();
}
