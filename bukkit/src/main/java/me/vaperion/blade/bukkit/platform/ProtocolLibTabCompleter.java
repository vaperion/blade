package me.vaperion.blade.bukkit.platform;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.vaperion.blade.Blade;
import me.vaperion.blade.bukkit.context.BukkitSender;
import me.vaperion.blade.platform.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProtocolLibTabCompleter extends PacketAdapter implements TabCompleter {

    private Blade blade;

    public ProtocolLibTabCompleter(@NotNull JavaPlugin plugin) {
        super(plugin, PacketType.Play.Client.TAB_COMPLETE);
    }

    @Override
    public void init(@NotNull Blade blade) {
        this.blade = blade;
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPlayer() == null) return;

        Player player = event.getPlayer();
        String commandLine = event.getPacket().getStrings().read(0);

        if (!commandLine.startsWith("/")) return;
        else commandLine = commandLine.substring(1);

        List<String> suggestions = blade.getCompleter().suggest(commandLine, () -> new BukkitSender(player));
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