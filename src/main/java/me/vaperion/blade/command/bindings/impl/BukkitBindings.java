package me.vaperion.blade.command.bindings.impl;

import me.vaperion.blade.command.argument.BladeArgument;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.bindings.Binding;
import me.vaperion.blade.command.context.BladeContext;
import me.vaperion.blade.command.exception.BladeExitMessage;
import me.vaperion.blade.command.exception.BladeUsageMessage;
import me.vaperion.blade.command.service.BladeCommandService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class BukkitBindings implements Binding {

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    @Override
    public void bind(@NotNull BladeCommandService commandService) {
        commandService.bindProvider(Player.class, new BladeProvider<Player>() {
            @Nullable
            @Override
            public Player provide(@NotNull BladeContext ctx, @NotNull BladeArgument arg) throws BladeExitMessage {
                Player player = ctx.sender().parseAs(Player.class);

                if (arg.getType() == BladeArgument.Type.OPTIONAL && "self".equals(arg.getString())) {
                    if (player != null) return player;
                    else throw new BladeUsageMessage(); // show usage to console if we have 'self' as a default value (only works on players)
                }

                Player onlinePlayer = getPlayer(arg.getString().trim());
                if (onlinePlayer == null && !arg.getParameter().defaultsToNull())
                    throw new BladeExitMessage("Error: No online player with name or UUID '" + arg.getString() + "' found.");

                return onlinePlayer;
            }

            @NotNull
            @Override
            public List<String> suggest(@NotNull BladeContext context, @NotNull BladeArgument arg) throws BladeExitMessage {
                Player sender = context.sender().parseAs(Player.class);
                List<String> completions = new ArrayList<>();

                String input = arg.getString().trim();

                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    if ((input.isEmpty() || player.getName().toLowerCase().startsWith(input.toLowerCase())) && (sender == null || sender.canSee(player)))
                        completions.add(player.getName());
                }

                return completions;
            }
        });

        commandService.bindProvider(OfflinePlayer.class, new BladeProvider<OfflinePlayer>() {
            @Nullable
            @Override
            public OfflinePlayer provide(@NotNull BladeContext ctx, @NotNull BladeArgument arg) throws BladeExitMessage {
                Player player = ctx.sender().parseAs(Player.class);

                if (arg.getType() == BladeArgument.Type.OPTIONAL && "self".equals(arg.getString())) {
                    if (player != null) return player;
                    else throw new BladeUsageMessage(); // show usage to console if we have 'self' as a default value (only works on players)
                }

                return getOfflinePlayer(arg.getString());
            }

            @NotNull
            @Override
            public List<String> suggest(@NotNull BladeContext context, @NotNull BladeArgument arg) throws BladeExitMessage {
                Player sender = context.sender().parseAs(Player.class);
                List<String> completions = new ArrayList<>();

                String input = arg.getString().trim();

                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    if ((input.isEmpty() || player.getName().toLowerCase().startsWith(input.toLowerCase())) && (sender == null || sender.canSee(player)))
                        completions.add(player.getName());
                }

                return completions;
            }
        });

        commandService.bindProvider(GameMode.class, new BladeProvider<GameMode>() {
            @Nullable
            @Override
            public GameMode provide(@NotNull BladeContext ctx, @NotNull BladeArgument arg) throws BladeExitMessage {
                GameMode mode = getGameMode(arg.getString().trim());

                if (mode == null && !arg.getParameter().defaultsToNull())
                    throw new BladeExitMessage("Error: '" + arg.getString() + "' is not a valid gamemode.");

                return mode;
            }

            @NotNull
            @Override
            public List<String> suggest(@NotNull BladeContext context, @NotNull BladeArgument arg) throws BladeExitMessage {
                String input = arg.getString().toUpperCase(Locale.ROOT);
                List<String> completions = new ArrayList<>();

                for (GameMode mode : GameMode.values()) {
                    if (mode.name().startsWith(input)) {
                        completions.add(mode.name().toLowerCase(Locale.ROOT));
                    }
                }

                return completions;
            }
        });
    }

    private boolean isUUID(@NotNull String input) {
        return UUID_PATTERN.matcher(input).matches();
    }

    @Nullable
    private Player getPlayer(@NotNull String input) {
        if (isUUID(input)) return Bukkit.getPlayer(UUID.fromString(input));
        return Bukkit.getPlayer(input);
    }

    @NotNull
    private OfflinePlayer getOfflinePlayer(@NotNull String input) {
        if (isUUID(input)) return Bukkit.getOfflinePlayer(UUID.fromString(input));
        return Bukkit.getOfflinePlayer(input);
    }

    @Nullable
    private GameMode getGameMode(String input) {
        input = input.toUpperCase(Locale.ROOT);

        for (GameMode mode : GameMode.values()) {
            if (mode.name().startsWith(input) || input.equals(String.valueOf(mode.getValue()))) {
                return mode;
            }
        }

        return null;
    }
}
