package me.vaperion.blade.bukkit;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.bukkit.argument.OfflinePlayerArgument;
import me.vaperion.blade.bukkit.argument.PlayerArgument;
import me.vaperion.blade.bukkit.container.BukkitContainer;
import me.vaperion.blade.bukkit.platform.BukkitHelpGenerator;
import me.vaperion.blade.bukkit.platform.BukkitLogger;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.impl.suggestions.SuggestionType;
import me.vaperion.blade.log.BladeLogger;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.BladePlatform;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Locale;

@RequiredArgsConstructor
public class BladeBukkitPlatform implements BladePlatform<String, Plugin, Server> {

    private static final Method SYNC_COMMANDS;

    static {
        Method syncCommands = null;

        try {
            Class<?> craftServerClass = Bukkit.getServer().getClass();
            syncCommands = craftServerClass.getDeclaredMethod("syncCommands");
            syncCommands.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
            // Doesn't exist in 1.8
        } catch (Throwable t) {
            BladeLogger.DEFAULT.error(t, "Failed to grab CraftServer#syncCommands method.");
        }

        SYNC_COMMANDS = syncCommands;
    }

    protected final Plugin plugin;
    protected Blade blade;
    protected EnumSet<SuggestionType> suggestionTypes = EnumSet.allOf(SuggestionType.class);

    @Override
    public void ingestBlade(@NotNull Blade blade) {
        this.blade = blade;
    }

    @ApiStatus.Internal
    @NotNull
    public EnumSet<SuggestionType> suggestionTypes() {
        return suggestionTypes;
    }

    @Override
    public @NotNull Server server() {
        return Bukkit.getServer();
    }

    @Override
    public @NotNull Plugin plugin() {
        return plugin;
    }

    @Override
    public @NotNull ContainerCreator<?> containerCreator(@NotNull BladeCommand command) {
        return BukkitContainer.CREATOR;
    }

    @Override
    public void configure(Blade.@NotNull Builder<String, Plugin, Server> builder,
                          @NotNull BladeConfiguration<String> configuration) {
        configuration.commandQualifier(plugin.getName().toLowerCase(Locale.ROOT));
        configuration.helpGenerator(new BukkitHelpGenerator());
        configuration.logger(new BukkitLogger(this));

        Binder<String, Plugin, Server> binder = new Binder<>(builder, true);
        binder.bind(Player.class, new PlayerArgument());
        binder.bind(OfflinePlayer.class, new OfflinePlayerArgument());
    }

    @Override
    public void triggerBrigadierSync() {
        if (SYNC_COMMANDS != null) {
            try {
                SYNC_COMMANDS.invoke(Bukkit.getServer());
            } catch (Throwable t) {
                blade.logger().error(t, "Failed to invoke CraftServer#syncCommands method, Brigadier may not recognize new commands.");
            }
        }
    }

    @Override
    public @NotNull String convertSenderTypeToName(@NotNull Class<?> type, boolean plural) {
        if (ConsoleCommandSender.class.isAssignableFrom(type)) {
            return "console";
        } else if (Player.class.isAssignableFrom(type)) {
            return plural ? "players" : "player";
        } else {
            // Fallback
            String name = type.getSimpleName().toLowerCase(Locale.ROOT);
            return plural ? name + "s" : name;
        }
    }
}
