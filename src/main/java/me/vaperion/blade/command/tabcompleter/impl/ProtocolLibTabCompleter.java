package me.vaperion.blade.command.tabcompleter.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.vaperion.blade.command.context.impl.BukkitSender;
import me.vaperion.blade.command.service.BladeCommandService;
import me.vaperion.blade.command.tabcompleter.TabCompleter;
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

        if (!commandLine.startsWith("/")) return;
        else commandLine = commandLine.substring(1);

        List<String> suggestions = commandService.getCommandCompleter().suggest(commandLine, () -> new BukkitSender(player), cmd -> hasPermission(player, cmd));
        if (suggestions == null) return; // if command was not found

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
}
