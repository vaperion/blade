package me.vaperion.blade.platform;

import me.vaperion.blade.Blade;
import me.vaperion.blade.Blade.Builder;
import me.vaperion.blade.container.ContainerCreator;
import org.jetbrains.annotations.NotNull;

public interface BladePlatform {
    @NotNull
    Object getPluginInstance();

    @NotNull
    ContainerCreator<?> getContainerCreator();

    void configureBlade(@NotNull Builder builder, @NotNull BladeConfiguration configuration);

    default void ingestBlade(@NotNull Blade blade) {
    }

    default void postCommandMapUpdate() {
    }
}
