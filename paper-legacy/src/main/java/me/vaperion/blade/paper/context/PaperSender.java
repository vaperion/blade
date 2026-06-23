package me.vaperion.blade.paper.context;

import me.vaperion.blade.bukkit.context.BukkitSender;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Paper sender that sends Components through the server's native Adventure support.
 */
public final class PaperSender extends BukkitSender {

    public PaperSender(@NotNull CommandSender commandSender) {
        super(commandSender);
    }

    @Override
    public void sendMessage(@NotNull Component component) {
        rawSender().sendMessage(component);
    }
}
