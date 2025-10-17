package me.vaperion.blade.fabric.util;

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("unused")
public final class TextUtil {

    @NotNull
    public static String toRaw(@NotNull Text text) {
        StringBuilder builder = new StringBuilder();

        text.visit(v -> {
            builder.append(v);
            return Optional.empty();
        });

        return builder.toString();
    }

    @NotNull
    public static Text fromLegacy(@NotNull String message) {
        String translated = translateColorCodes(message);
        return Text.literal(translated);
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
