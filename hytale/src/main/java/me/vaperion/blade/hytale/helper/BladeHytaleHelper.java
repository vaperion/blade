package me.vaperion.blade.hytale.helper;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

@SuppressWarnings("unused")
public final class BladeHytaleHelper {

    private BladeHytaleHelper() {
    }

    /**
     * Get the {@link EntityStore} reference from a {@link PlayerRef}.
     *
     * @param playerRef the player reference
     * @return the entity store reference
     */
    @UnknownNullability
    public static Ref<EntityStore> ref(@NotNull PlayerRef playerRef) {
        return playerRef.getReference();
    }

    /**
     * Get the {@link EntityStore} reference from a {@link Player}.
     *
     * @param player the player
     * @return the entity store reference
     */
    @UnknownNullability
    public static Ref<EntityStore> ref(@NotNull Player player) {
        return player.getReference();
    }

    /**
     * Get the {@link EntityStore} from a {@link PlayerRef}.
     *
     * @param playerRef the player reference
     * @return the entity store
     */
    @UnknownNullability
    public static Store<EntityStore> store(@NotNull PlayerRef playerRef) {
        Ref<EntityStore> ref = ref(playerRef);
        return ref.isValid() ? ref.getStore() : null;
    }

    /**
     * Get the {@link EntityStore} from a {@link Player}.
     *
     * @param player the player
     * @return the entity store
     */
    @UnknownNullability
    public static Store<EntityStore> store(@NotNull Player player) {
        Ref<EntityStore> ref = ref(player);
        return ref.isValid() ? ref.getStore() : null;
    }

    /**
     * Get the {@link World} from a {@link Store<EntityStore>}.
     *
     * @param store the entity store
     * @return the world
     */
    @UnknownNullability
    public static World world(@NotNull Store<EntityStore> store) {
        return store.getExternalData().getWorld();
    }

    /**
     * Get the {@link World} from a {@link PlayerRef}.
     *
     * @param playerRef the player ref
     * @return the world
     */
    @UnknownNullability
    public static World world(@NotNull PlayerRef playerRef) {
        return world(store(playerRef));
    }

    /**
     * Get the {@link PlayerRef} from a {@link Ref<EntityStore>}.
     *
     * @param ref the entity store reference
     * @return the player reference
     */
    @UnknownNullability
    public static PlayerRef playerRef(@NotNull Ref<EntityStore> ref) {
        if (!ref.isValid()) return null;
        Store<EntityStore> store = ref.getStore();
        return store.getComponent(ref, PlayerRef.getComponentType());
    }

    /**
     * Get the {@link PlayerRef} from a {@link Player}.
     *
     * @param player the player
     * @return the player reference
     */
    @UnknownNullability
    public static PlayerRef playerRef(@NotNull Player player) {
        return playerRef(ref(player));
    }

    /**
     * Get the {@link Player} from a {@link PlayerRef}.
     *
     * @param playerRef the player reference
     * @return the player
     */
    @UnknownNullability
    public static Player player(@NotNull PlayerRef playerRef) {
        Store<EntityStore> store = store(playerRef);
        if (store == null) return null;

        Ref<EntityStore> ref = ref(playerRef);
        return store.getComponent(ref, Player.getComponentType());
    }

}
