package me.vaperion.blade.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import me.vaperion.blade.command.BladeParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Maps command parameters to platform-native Brigadier argument types.
 * <p>
 * Blade only knows the vanilla types. A platform can supply richer client-side
 * types for its own classes, such as a resource key argument.
 */
@FunctionalInterface
public interface BrigadierArgumentTypeMapper {

    /**
     * Resolves the native argument type for a parameter.
     *
     * @param parameter the command parameter
     * @return the native argument type, or null to use Blade's default mapping
     */
    @Nullable
    ArgumentType<?> map(@NotNull BladeParameter parameter);

    /**
     * A mapper that defers every parameter to Blade's default mapping.
     *
     * @return the no-op mapper
     */
    @NotNull
    static BrigadierArgumentTypeMapper none() {
        return parameter -> null;
    }
}
