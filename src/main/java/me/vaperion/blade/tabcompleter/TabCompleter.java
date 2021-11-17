package me.vaperion.blade.tabcompleter;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.service.BladeCommandService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface TabCompleter {
    void init(@NotNull BladeCommandService commandService);

    default boolean isDefault() {
        return false;
    }

    default boolean hasPermission(@NotNull Player player, @NotNull BladeCommand command) {
        if ("op".equals(command.getPermission())) return player.isOp();
        if (command.getPermission() == null || command.getPermission().trim().isEmpty()) return true;
        return player.hasPermission(command.getPermission());
    }
}
