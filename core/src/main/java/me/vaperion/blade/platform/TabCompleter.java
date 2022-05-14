package me.vaperion.blade.platform;

import me.vaperion.blade.Blade;
import org.jetbrains.annotations.NotNull;

public interface TabCompleter {
    void init(@NotNull Blade blade);

    default boolean isDefault() {
        return false;
    }

    final class Default implements TabCompleter {
        @Override
        public void init(@NotNull Blade blade) {
        }

        @Override
        public boolean isDefault() {
            return true;
        }
    }
}