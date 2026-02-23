package me.vaperion.blade.fabric.container;

import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.brigadier.BladeBrigadierBuilder;
import me.vaperion.blade.brigadier.BladeBrigadierDelegate;
import me.vaperion.blade.brigadier.BrigadierRichSuggestionsBuilder;
import me.vaperion.blade.fabric.context.FabricSender;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@Getter
public final class BladeFabricBrigadier {

    private final Blade blade;
    private final BladeBrigadierBuilder<ServerCommandSource, ServerCommandSource> builder;
    private final BladeBrigadierDelegate<ServerCommandSource, FabricContainer> delegate;

    public BladeFabricBrigadier(@NotNull Blade blade) {
        this.blade = blade;

        this.builder = new BladeBrigadierBuilder<>(blade,
            Function.identity(),
            s -> new FabricSender(blade, s));

        this.delegate = new BladeBrigadierDelegate<>(blade,
            (ctx, builder, container) ->
                container.suggest(ctx, new BrigadierRichSuggestionsBuilder(builder)),
            (ctx, container) -> container.execute(ctx)
        );
    }

}
