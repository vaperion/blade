package me.vaperion.blade.bindings;

import me.vaperion.blade.service.BladeCommandService;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Binding {
    void bind(@NotNull BladeCommandService commandService);
}
