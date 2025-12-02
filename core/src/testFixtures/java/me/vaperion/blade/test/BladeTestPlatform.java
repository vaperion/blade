package me.vaperion.blade.test;

import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.BladePlatform;
import me.vaperion.blade.test.container.TestContainer;
import me.vaperion.blade.test.platform.TestPlugin;
import me.vaperion.blade.test.platform.TestServer;
import org.jetbrains.annotations.NotNull;

public final class BladeTestPlatform implements BladePlatform<String, TestPlugin, TestServer> {

    @NotNull
    public static Blade createInstance() {
        return Blade.forPlatform(new BladeTestPlatform()).build();
    }

    @Override
    public @NotNull TestPlugin plugin() {
        return TestPlugin.TEST;
    }

    @Override
    public @NotNull TestServer server() {
        return TestServer.TEST;
    }

    @Override
    public @NotNull ContainerCreator<?> containerCreator(@NotNull BladeCommand command) {
        return TestContainer.CREATOR;
    }

    @Override
    public void configure(Blade.@NotNull Builder<String, TestPlugin, TestServer> builder,
                          @NotNull BladeConfiguration<String> configuration) {
        // No-op
    }

    @Override
    public @NotNull String convertSenderTypeToName(@NotNull Class<?> type, boolean plural) {
        return type.getName();
    }
}
