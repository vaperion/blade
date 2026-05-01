package me.vaperion.blade.fabric.context;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Sender;
import me.vaperion.blade.fabric.BladeFabricPlatform;
import me.vaperion.blade.fabric.ext.CommandSourceStackExt;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public final class FabricSender implements Sender<CommandSourceStack> {
    private final Blade blade;
    private final CommandSourceStack commandSource;

    @NotNull
    @Override
    public CommandSourceStack rawSender() {
        return commandSource;
    }

    @Override
    public @NotNull Object underlyingSender() {
        var entity = commandSource.getEntity();
        return entity != null ? entity : commandSource;
    }

    @Override
    public @NotNull Class<?> underlyingSenderType() {
        return commandSource.getEntity() != null
            ? commandSource.getEntity().getClass()
            : CommandSourceStack.class;
    }

    @NotNull
    @Override
    public String name() {
        return commandSource.getTextName();
    }

    @Override
    public boolean hasPermission(@NotNull String permissionNode) {
        var isConsole = ((CommandSourceStackExt) commandSource).blade$isConsole();

        if ("op".equals(permissionNode))
            return isConsole || commandSource.hasPermission(4);
        if ("console".equals(permissionNode))
            return isConsole;

        return blade.platformAs(BladeFabricPlatform.class)
            .permissionsProvider()
            .hasPermission(commandSource, permissionNode);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T parseAs(@NotNull Class<T> clazz) {
        // We do exact comparisons instead of isAssignableFrom / others here on purpose
        var entity = commandSource.getEntity();

        if (clazz.equals(ServerPlayer.class) && entity instanceof ServerPlayer)
            return (T) entity;
        else if (clazz.equals(Entity.class) && entity != null)
            return (T) entity;
        else if (clazz.equals(CommandSourceStack.class) || clazz.equals(SharedSuggestionProvider.class))
            return (T) commandSource;

        return null;
    }

    @Override
    public boolean isExpectedType(@NotNull BladeCommand command) {
        return parseAs(command.senderType()) != null;
    }
}
