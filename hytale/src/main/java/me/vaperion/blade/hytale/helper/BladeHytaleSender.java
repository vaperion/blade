package me.vaperion.blade.hytale.helper;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

@SuppressWarnings("unused")
public record BladeHytaleSender(@UnknownNullability Store<EntityStore> entityStore,
                                @UnknownNullability Ref<EntityStore> entityRef,
                                @UnknownNullability Player player,
                                @UnknownNullability PlayerRef playerRef) {

    @NotNull
    public static BladeHytaleSender of(@NotNull Player player) {
        Store<EntityStore> store = BladeHytaleHelper.store(player);
        Ref<EntityStore> ref = BladeHytaleHelper.ref(player);
        PlayerRef playerRef = BladeHytaleHelper.playerRef(player);
        return new BladeHytaleSender(store, ref, player, playerRef);
    }

    @NotNull
    public static BladeHytaleSender of(@NotNull PlayerRef playerRef) {
        Store<EntityStore> store = BladeHytaleHelper.store(playerRef);
        Ref<EntityStore> ref = BladeHytaleHelper.ref(playerRef);
        Player player = BladeHytaleHelper.player(playerRef);
        return new BladeHytaleSender(store, ref, player, playerRef);
    }

    @NotNull
    public static BladeHytaleSender of(@NotNull ConsoleSender consoleSender) {
        return new BladeHytaleSender(null, null, null, null);
    }

}
