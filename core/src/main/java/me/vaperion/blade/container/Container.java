package me.vaperion.blade.container;

import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface Container {
    /**
     * Gets the Blade instance associated with this container.
     *
     * @return the Blade instance
     */
    @NotNull Blade blade();

    /**
     * Gets the base command associated with this container.
     *
     * @return the base command
     */
    @NotNull BladeCommand baseCommand();
}
