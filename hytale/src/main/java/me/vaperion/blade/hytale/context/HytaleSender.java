package me.vaperion.blade.hytale.context;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Sender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class HytaleSender implements Sender<CommandSender> {
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
        return commandSender.getDisplayName();
    }

    @Override
    public boolean hasPermission(@NotNull String permissionNode) {
        if ("console".equals(permissionNode))
            return commandSender instanceof ConsoleSender;

        return commandSender.hasPermission(permissionNode);
    }

    @SuppressWarnings({ "unchecked", "removal" })
    @Nullable
    @Override
    public <T> T parseAs(@NotNull Class<T> clazz) {
        // We do exact comparisons instead of isAssignableFrom / others here on purpose

        if (clazz.equals(Player.class) && commandSender instanceof Player)
            return (T) commandSender;
        else if (clazz.equals(PlayerRef.class) && commandSender instanceof Player)
            return (T) ((Player) commandSender).getPlayerRef();
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
