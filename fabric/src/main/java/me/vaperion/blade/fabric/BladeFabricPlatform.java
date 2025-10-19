package me.vaperion.blade.fabric;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.fabric.argument.ServerPlayerEntityArgument;
import me.vaperion.blade.fabric.container.BladeFabricBrigadier;
import me.vaperion.blade.fabric.container.FabricContainer;
import me.vaperion.blade.fabric.platform.FabricHelpGenerator;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.BladePlatform;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@RequiredArgsConstructor
public class BladeFabricPlatform implements BladePlatform<Text, ModContainer, MinecraftServer> {

    protected final ModContainer mod;
    protected Blade blade;

    @Getter
    private BladeFabricBrigadier brigadier;

    @Override
    public void ingestBlade(@NotNull Blade blade) {
        this.blade = blade;

        brigadier = new BladeFabricBrigadier(blade);

        BladeFabricGlobal.ACTIVE_INSTANCES.add(blade);
    }

    @Override
    public @NotNull MinecraftServer server() {
        MinecraftServer server = BladeFabricGlobal.SERVER;

        if (server == null) {
            throw new IllegalStateException("MinecraftServer instance is not available yet.");
        }

        return server;
    }

    @Override
    public @NotNull ModContainer plugin() {
        return mod;
    }

    @Override
    public @NotNull ContainerCreator<?> containerCreator() {
        return FabricContainer.CREATOR;
    }

    @Override
    public void configure(Blade.@NotNull Builder<Text, ModContainer, MinecraftServer> builder,
                          @NotNull BladeConfiguration<Text> configuration) {
        configuration.commandQualifier(mod.getMetadata().getName().toLowerCase(Locale.ROOT));
        configuration.helpGenerator(new FabricHelpGenerator());

        Binder<Text, ModContainer, MinecraftServer> binder = new Binder<>(builder, true);
        binder.bind(ServerPlayerEntity.class, new ServerPlayerEntityArgument());
    }

    @Override
    public @NotNull String convertSenderTypeToName(@NotNull Class<?> type, boolean plural) {
        if (ServerPlayerEntity.class.isAssignableFrom(type)) {
            return plural ? "players" : "player";
        } else {
            // Fallback
            String name = type.getSimpleName().toLowerCase(Locale.ROOT);
            return plural ? name + "s" : name;
        }
    }
}
