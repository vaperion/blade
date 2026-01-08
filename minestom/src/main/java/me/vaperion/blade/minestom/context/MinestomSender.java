package me.vaperion.blade.minestom.context;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Sender;
import me.vaperion.blade.minestom.BladeMinestomGlobal;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class MinestomSender implements Sender<CommandSender> {
    private final CommandSender commandSender;

    @Override
    public @NotNull CommandSender rawSender() {
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

    @Override
    public @NotNull String name() {
        if (commandSender instanceof Player)
            return ((Player) commandSender).getUsername();
        else if (commandSender instanceof ConsoleSender)
            return "Console";
        return "<unknown>";
    }

    @Override
    public boolean hasPermission(@NotNull String permissionNode) {
        if ("console".equals(permissionNode))
            return commandSender instanceof ConsoleSender;

        return BladeMinestomGlobal.hasPermission(this, permissionNode);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T parseAs(@NotNull Class<T> clazz) {
        // We do exact comparisons instead of isAssignableFrom / others here on purpose

        if (clazz.equals(Player.class) && commandSender instanceof Player)
            return (T) commandSender;
        else if (clazz.equals(ConsoleSender.class) && commandSender instanceof ConsoleSender)
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
