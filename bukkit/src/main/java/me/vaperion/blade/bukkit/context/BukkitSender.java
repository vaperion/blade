package me.vaperion.blade.bukkit.context;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.context.WrappedSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public final class BukkitSender implements WrappedSender<CommandSender> {
    private final CommandSender commandSender;

    @NotNull
    @Override
    public CommandSender getSender() {
        return commandSender;
    }

    @NotNull
    @Override
    public String getName() {
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

    @Override
    public void sendMessage(@NotNull String message) {
        commandSender.sendMessage(message);
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        commandSender.sendMessage(messages);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T parseAs(@NotNull Class<T> clazz) {
        if (clazz.equals(Player.class) && commandSender instanceof Player)
            return (T) commandSender;
        else if (clazz.equals(ConsoleCommandSender.class) && commandSender instanceof ConsoleCommandSender)
            return (T) commandSender;
        else if (clazz.equals(CommandSender.class))
            return (T) commandSender;

        return null;
    }
}