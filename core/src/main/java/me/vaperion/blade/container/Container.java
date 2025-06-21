package me.vaperion.blade.container;

import me.vaperion.blade.Blade;
import me.vaperion.blade.command.Command;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface Container {
    @NotNull Blade getBlade();

    @NotNull Command getBaseCommand();
}
