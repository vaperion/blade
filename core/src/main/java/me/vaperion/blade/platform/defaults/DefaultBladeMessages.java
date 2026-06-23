package me.vaperion.blade.platform.defaults;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.platform.api.BladeMessages;
import me.vaperion.blade.util.BladeHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

public class DefaultBladeMessages implements BladeMessages {

    @Override
    public @NotNull Component unknownCommand() {
        return text("Unknown command. Type \"/help\" for help.", NamedTextColor.WHITE);
    }

    @Override
    public @NotNull Component permissionMessage(@NotNull BladeCommand command) {
        return text(command.permissionMessage(), NamedTextColor.RED);
    }

    @Override
    public @NotNull Component noHelpPermission(@NotNull Context context) {
        return text(context.blade().configuration().defaultPermissionMessage(), NamedTextColor.RED);
    }

    @Override
    public @NotNull Component error(@NotNull String message) {
        return text(message, NamedTextColor.RED);
    }

    @Override
    public @NotNull Component genericError() {
        return text(BladeHelper.ERROR_MESSAGE, NamedTextColor.RED);
    }
}
