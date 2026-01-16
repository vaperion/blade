package me.vaperion.blade.hytale;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.hytale.argument.PlayerArgument;
import me.vaperion.blade.hytale.argument.PlayerRefArgument;
import me.vaperion.blade.hytale.command.HytaleCommandFeedback;
import me.vaperion.blade.hytale.container.HytaleContainer;
import me.vaperion.blade.hytale.platform.HytaleHelpGenerator;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.BladePlatform;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@RequiredArgsConstructor
public class BladeHytalePlatform implements BladePlatform<Message, PluginBase, HytaleServer> {

    protected final PluginBase plugin;

    protected Blade blade;

    @Override
    public void ingestBlade(@NotNull Blade blade) {
        this.blade = blade;
    }

    @Override
    public @NotNull HytaleServer server() {
        return HytaleServer.get();
    }

    @Override
    public @NotNull PluginBase plugin() {
        return plugin;
    }

    @Override
    public @NotNull ContainerCreator<?> containerCreator(@NotNull BladeCommand command) {
        return HytaleContainer.CREATOR;
    }

    @Override
    public void configure(Blade.@NotNull Builder<Message, PluginBase, HytaleServer> builder,
                          @NotNull BladeConfiguration<Message> configuration) {
        configuration.helpGenerator(new HytaleHelpGenerator());
        configuration.feedbackCreator(HytaleCommandFeedback::new);

        Binder<Message, PluginBase, HytaleServer> binder = new Binder<>(builder, true);
        binder.bind(Player.class, new PlayerArgument());
        binder.bind(PlayerRef.class, new PlayerRefArgument());
    }

    @Override
    public @NotNull String convertSenderTypeToName(@NotNull Class<?> type, boolean plural) {
        if (Player.class.isAssignableFrom(type) || PlayerRef.class.isAssignableFrom(type)) {
            return plural ? "players" : "player";
        } else if (ConsoleSender.class.isAssignableFrom(type)) {
            return "console";
        } else {
            // Fallback
            String name = type.getSimpleName().toLowerCase(Locale.ROOT);
            return plural ? name + "s" : name;
        }
    }
}
