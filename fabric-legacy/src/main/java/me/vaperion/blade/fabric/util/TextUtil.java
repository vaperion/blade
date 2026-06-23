package me.vaperion.blade.fabric.util;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("unused")
public final class TextUtil {

    /**
     * Convert a Kyori component to a native component.
     *
     * @param adventure  the Kyori component
     * @param registries the source's registry/holder lookup (for registry-backed payloads)
     * @return the equivalent Minecraft component
     */
    @NotNull
    public static Component fromAdventure(@NotNull net.kyori.adventure.text.Component adventure,
                                          @NotNull HolderLookup.Provider registries) {
        String json = GsonComponentSerializer.gson().serialize(adventure);

        return ComponentSerialization.CODEC
            .parse(registries.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
            .getOrThrow();
    }

    @NotNull
    public static String toRaw(@NotNull Component text) {
        StringBuilder builder = new StringBuilder();

        text.visit(v -> {
            builder.append(v);
            return Optional.empty();
        });

        return builder.toString();
    }

    @NotNull
    public static Component fromLegacy(@NotNull String message) {
        String translated = translateColorCodes(message);
        return Component.literal(translated);
    }

    @NotNull
    public static String translateColorCodes(@NotNull String message) {
        return message.replaceAll("&([0-9a-fk-or])", "§$1");
    }

    @NotNull
    public static String stripColorCodes(@NotNull String message) {
        return message.replaceAll("§[0-9a-fk-or]", "");
    }

}
