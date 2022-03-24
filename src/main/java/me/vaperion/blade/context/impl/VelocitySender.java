package me.vaperion.blade.context.impl;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.context.WrappedSender;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class VelocitySender implements WrappedSender<CommandSource> {
    private final CommandSource commandSource;

    @Override
    public @NotNull CommandSource getBackingSender() {
        return commandSource;
    }

    @Override
    public @NotNull String getName() {
        if (commandSource instanceof Player)
            return ((Player) commandSource).getUsername();
        else if (commandSource instanceof ConsoleCommandSource)
            return "Console";
        return "<unknown>";
    }

    @Override
    public boolean hasPermission(@NotNull String permissionNode) {
        if ("console".equals(permissionNode))
            return commandSource instanceof ConsoleCommandSource;
        return commandSource.hasPermission(permissionNode);
    }

    @Override
    public void sendMessage(@NotNull String message) {
        commandSource.sendMessage(Component.text(message));
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        for (String message : messages)
            sendMessage(message);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T parseAs(@NotNull Class<T> clazz) {
        if (clazz.equals(Player.class) && commandSource instanceof Player)
            return (T) commandSource;
        else if (clazz.equals(ConsoleCommandSource.class) && commandSource instanceof ConsoleCommandSource)
            return (T) commandSource;
        else if (clazz.equals(CommandSource.class))
            return (T) commandSource;

        return null;
    }
}
