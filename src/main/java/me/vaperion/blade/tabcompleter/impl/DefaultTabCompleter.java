package me.vaperion.blade.tabcompleter.impl;

import me.vaperion.blade.service.BladeCommandService;
import me.vaperion.blade.tabcompleter.TabCompleter;
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
