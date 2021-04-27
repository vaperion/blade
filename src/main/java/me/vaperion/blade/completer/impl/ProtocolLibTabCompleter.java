package me.vaperion.blade.completer.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.vaperion.blade.command.container.BladeCommand;
import me.vaperion.blade.command.context.impl.BukkitSender;
import me.vaperion.blade.command.service.BladeCommandService;
import me.vaperion.blade.completer.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProtocolLibTabCompleter extends PacketAdapter implements TabCompleter {

    private BladeCommandService commandService;

    public ProtocolLibTabCompleter(@NotNull JavaPlugin plugin) {
        super(plugin, PacketType.Play.Client.TAB_COMPLETE);
    }

    @Override
    public void init(@NotNull BladeCommandService commandService) {
        this.commandService = commandService;
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPlayer() == null) return;

        Player player = event.getPlayer();
        String commandLine = event.getPacket().getStrings().read(0);
        List<String> suggestions = commandService.getCommandCompleter().suggest(commandLine, () -> new BukkitSender(player), cmd -> hasPermission(player, cmd));

        try {
            event.setCancelled(true);
            PacketContainer tabComplete = new PacketContainer(PacketType.Play.Server.TAB_COMPLETE);
            tabComplete.getStringArrays().write(0, suggestions.toArray(new String[0]));
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, tabComplete);
        } catch (Exception ex) {
            System.err.println("An exception was thrown while attempting to tab complete '" + commandLine + "' for player " + player.getName());
            ex.printStackTrace();
        }
    }

    private boolean hasPermission(@NotNull Player player, @NotNull BladeCommand command) {
        if ("op".equals(command.getPermission())) return player.isOp();
        if (command.getPermission() == null || command.getPermission().trim().isEmpty()) return true;
        return player.hasPermission(command.getPermission());
    }
}
