package me.vaperion.blade.paper.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import me.vaperion.blade.brigadier.BrigadierArgumentTypeMapper;
import me.vaperion.blade.command.BladeParameter;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Registers parameters whose grammar matches a vanilla argument as Paper's native type.
 */
@SuppressWarnings("UnstableApiUsage")
final class PaperArgumentTypes implements BrigadierArgumentTypeMapper {

    @Override
    public @Nullable ArgumentType<?> map(@NotNull BladeParameter parameter) {
        Class<?> type = parameter.type();

        if (type == Key.class) {
            return ArgumentTypes.key();
        }

        if (type == NamespacedKey.class) {
            return ArgumentTypes.namespacedKey();
        }

        if (type == UUID.class) {
            return ArgumentTypes.uuid();
        }

        return null;
    }
}
