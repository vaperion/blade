package me.vaperion.blade.velocity.context;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Sender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class VelocitySender implements Sender<CommandSource> {
    private final CommandSource commandSource;

    @Override
    public @NotNull CommandSource rawSender() {
        return commandSource;
    }

    @Override
    public @NotNull Object underlyingSender() {
        return commandSource;
    }

    @Override
    public @NotNull Class<?> underlyingSenderType() {
        return commandSource.getClass();
    }

    @Override
    public @NotNull String name() {
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

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T parseAs(@NotNull Class<T> clazz) {
        // We do exact comparisons instead of isAssignableFrom / others here on purpose

        if (clazz.equals(Player.class) && commandSource instanceof Player)
            return (T) commandSource;
        else if (clazz.equals(ConsoleCommandSource.class) && commandSource instanceof ConsoleCommandSource)
            return (T) commandSource;
        else if (clazz.equals(CommandSource.class))
            return (T) commandSource;

        return null;
    }

    @Override
    public boolean isExpectedType(@NotNull BladeCommand command) {
        return parseAs(command.senderType()) != null;
    }
}