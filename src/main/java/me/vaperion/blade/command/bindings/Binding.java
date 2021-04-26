package me.vaperion.blade.command.bindings;

import me.vaperion.blade.command.service.BladeCommandService;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Binding {
    void bind(@NotNull BladeCommandService commandService);
}
