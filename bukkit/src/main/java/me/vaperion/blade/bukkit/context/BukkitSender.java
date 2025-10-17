package me.vaperion.blade.bukkit.context;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Sender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public final class BukkitSender implements Sender<CommandSender> {
    private final CommandSender commandSender;

    @NotNull
    @Override
    public CommandSender rawSender() {
        return commandSender;
    }

    @Override
    public @NotNull Object underlyingSender() {
        return commandSender;
    }

    @Override
    public @NotNull Class<?> underlyingSenderType() {
        return commandSender.getClass();
    }

    @NotNull
    @Override
    public String name() {
        return commandSender.getName();
    }

    @Override
    public boolean hasPermission(@NotNull String permissionNode) {
        if ("op".equals(permissionNode))
            return commandSender instanceof ConsoleCommandSender || commandSender.isOp();
        if ("console".equals(permissionNode))
            return commandSender instanceof ConsoleCommandSender;
        return commandSender.hasPermission(permissionNode);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T parseAs(@NotNull Class<T> clazz) {
        // We do exact comparisons instead of isAssignableFrom / others here on purpose

        if (clazz.equals(Player.class) && commandSender instanceof Player)
            return (T) commandSender;
        else if (clazz.equals(ConsoleCommandSender.class) && commandSender instanceof ConsoleCommandSender)
            return (T) commandSender;
        else if (clazz.equals(CommandSender.class))
            return (T) commandSender;

        return null;
    }

    @Override
    public boolean isExpectedType(@NotNull BladeCommand command) {
        return parseAs(command.senderType()) != null;
    }
}