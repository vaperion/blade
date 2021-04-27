package me.vaperion.blade.completer;

import me.vaperion.blade.command.service.BladeCommandService;
import org.jetbrains.annotations.NotNull;

public interface TabCompleter {
    void init(@NotNull BladeCommandService commandService);

    default boolean isDefault() {
        return false;
    }
}
