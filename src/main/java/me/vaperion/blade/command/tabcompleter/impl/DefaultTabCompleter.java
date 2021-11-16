package me.vaperion.blade.command.tabcompleter.impl;

import me.vaperion.blade.command.service.BladeCommandService;
import me.vaperion.blade.command.tabcompleter.TabCompleter;
import org.jetbrains.annotations.NotNull;

public class DefaultTabCompleter implements TabCompleter {
    @Override
    public void init(@NotNull BladeCommandService commandService) {
        // noop
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}
