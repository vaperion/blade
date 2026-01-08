package me.vaperion.blade.container;

import me.vaperion.blade.Blade;
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
     * Unregisters this container from the command system.
     */
    void unregister();
}
