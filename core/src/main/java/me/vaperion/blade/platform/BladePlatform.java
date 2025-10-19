package me.vaperion.blade.platform;

import me.vaperion.blade.Blade;
import me.vaperion.blade.Blade.Builder;
import me.vaperion.blade.container.ContainerCreator;
import org.jetbrains.annotations.NotNull;

public interface BladePlatform<Text, Plugin, Server> {
    /**
     * Gets the plugin instance.
     *
     * @return the plugin instance
     */
    @NotNull
    Plugin plugin();

    /**
     * Gets the server instance.
     *
     * @return the server instance
     */
    @NotNull
    Server server();

    /**
     * Gets the container creator.
     *
     * @return the container creator
     */
    @NotNull
    ContainerCreator<?> containerCreator();

    /**
     * Configures the platform with the given Blade builder and configuration.
     *
     * @param builder       the Blade builder
     * @param configuration the Blade configuration
     */
    void configure(@NotNull Builder<Text, Plugin, Server> builder,
                   @NotNull BladeConfiguration<Text> configuration);

    /**
     * Ingests the Blade instance after it has been built.
     *
     * @param blade the Blade instance
     */
    default void ingestBlade(@NotNull Blade blade) {
    }

    /**
     * Triggers a Brigadier sync for the platform, if applicable.
     */
    default void triggerBrigadierSync() {
    }

    /**
     * Converts the given type to a fancy, human-readable name for sender types.
     *
     * @param type   the type to convert
     * @param plural whether to return the plural form
     * @return the fancy name
     */
    @NotNull
    String convertSenderTypeToName(@NotNull Class<?> type, boolean plural);
}
