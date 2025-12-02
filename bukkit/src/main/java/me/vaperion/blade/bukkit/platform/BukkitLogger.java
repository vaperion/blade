package me.vaperion.blade.bukkit.platform;

import me.vaperion.blade.bukkit.BladeBukkitPlatform;
import me.vaperion.blade.log.BladeLogger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class BukkitLogger implements BladeLogger {

    private final BladeBukkitPlatform platform;

    public BukkitLogger(@NotNull BladeBukkitPlatform platform) {
        this.platform = platform;
    }

    @Override
    public void info(@NotNull String msg, @NotNull Object... args) {
        platform.plugin().getLogger().info("[Blade] " + String.format(msg, args));
    }

    @Override
    public void warn(@NotNull String msg, @NotNull Object... args) {
        platform.plugin().getLogger().warning("[Blade] " + String.format(msg, args));
    }

    @Override
    public void error(@NotNull String msg, @NotNull Object... args) {
        platform.plugin().getLogger().severe("[Blade] " + String.format(msg, args));
    }

    @Override
    public void error(@NotNull Throwable throwable,
                      @NotNull String msg,
                      @NotNull Object... args) {
        platform.plugin().getLogger().log(Level.SEVERE,
            "[Blade] " + String.format(msg, args),
            throwable);
    }

}
