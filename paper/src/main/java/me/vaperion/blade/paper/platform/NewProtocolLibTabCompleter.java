package me.vaperion.blade.paper.platform;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.wrappers.Converters;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import me.vaperion.blade.Blade;
import me.vaperion.blade.bukkit.context.BukkitSender;
import me.vaperion.blade.platform.TabCompleter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class NewProtocolLibTabCompleter extends PacketAdapter implements TabCompleter {

    private Blade blade;

    public NewProtocolLibTabCompleter(@NotNull JavaPlugin plugin) {
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
            int intCount = tabComplete.getIntegers().size();

            if (intCount == 0) {
                sendLegacyCompletions(player, suggestions, tabComplete);
            } else if (intCount == 1) {
                sendModernCompletions(player, commandLine, suggestions,
                    event.getPacket(), tabComplete);
            } else if (intCount == 3) {
                sendLatestCompletions(player, commandLine, suggestions,
                    event.getPacket(), tabComplete);
            } else {
                throw new UnsupportedOperationException("Unsupported tab complete packet structure, int count: " + intCount);
            }
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while attempting to tab complete '%s' for player %s!",
                commandLine, player.getName());
        }
    }

    private void sendLegacyCompletions(@NotNull Player player,
                                       @NotNull List<String> suggestions,
                                       @NotNull PacketContainer container) {
        // Used: 1.8-1.12.2
        // Packet structure: String[] (suggestions)

        container.getStringArrays().write(0, suggestions.toArray(new String[0]));
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);
    }

    private void sendModernCompletions(@NotNull Player player,
                                       @NotNull String commandLine,
                                       @NotNull List<String> suggestions,
                                       @NotNull PacketContainer received,
                                       @NotNull PacketContainer container) {
        // Used: 1.13-1.20.4
        // Packet structure: int (transaction id), Suggestions (suggestions)

        int rangeStart = commandLine.lastIndexOf(' ') + 1;
        int rangeEnd = commandLine.length();

        StringRange stringRange = StringRange.between(rangeStart + 1, rangeEnd + 1);

        List<Suggestion> entries = suggestions.stream()
            .map(suggestion -> new Suggestion(stringRange, suggestion))
            .toList();

        Suggestions brigadierSuggestions = new Suggestions(stringRange, entries);

        container.getIntegers().write(0, received.getIntegers().read(0)); // transaction id

        container.getSpecificModifier(Suggestions.class).write(0, brigadierSuggestions);

        ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);
    }

    private void sendLatestCompletions(@NotNull Player player,
                                       @NotNull String commandLine,
                                       @NotNull List<String> suggestions,
                                       @NotNull PacketContainer received,
                                       @NotNull PacketContainer container) {
        // Used: 1.20.5 and onwards
        // Packet structure: int (transaction id), int (start), int (end), List<Entry> (suggestions)

        int rangeStart = commandLine.lastIndexOf(' ') + 1;
        int rangeEnd = commandLine.length();

        List<SuggestionEntry> entries = suggestions.stream()
            .map(suggestion -> new SuggestionEntry(suggestion, Optional.empty()))
            .toList();

        container.getIntegers().write(0, received.getIntegers().read(0)); // transaction id

        container.getIntegers().write(1, rangeStart + 1); // start
        container.getIntegers().write(2, rangeEnd + 1); // end

        container.getLists(SuggestionEntry.converter()).write(0, entries);

        ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);
    }

    // Yes, this is terrible.
    // Blame ProtocolLib for not providing a wrapper for this type.
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    record SuggestionEntry(String text, Optional<Component> tooltip) {
        private static final List<String> CLASS_NAMES = List.of(
            "net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket$Entry"
        );

        @NotNull
        static EquivalentConverter<SuggestionEntry> converter() {
            return Converters.ignoreNull(Converters.handle(SuggestionEntry::toHandle,
                SuggestionEntry::fromHandle,
                SuggestionEntry.class));
        }

        @NotNull
        static Object toHandle(@NotNull SuggestionEntry entry) {
            Object handle = tryCreateHandle(entry.text(), entry.tooltip());
            if (handle == null) throw new UnsupportedOperationException("Failed to create handle for SuggestionEntry");

            return handle;
        }

        @NotNull
        static SuggestionEntry fromHandle(@NotNull Object object) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Nullable
        private static Object tryCreateHandle(@NotNull String text,
                                              @NotNull Optional<Component> tooltip) {
            for (String name : CLASS_NAMES) {
                try {
                    return Class.forName(name)
                        .getDeclaredConstructor(String.class, Optional.class)
                        .newInstance(text, tooltip);
                } catch (Throwable ignored) {
                }
            }

            return null;
        }
    }
}
