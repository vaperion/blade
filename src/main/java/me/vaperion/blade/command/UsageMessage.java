package me.vaperion.blade.command;

import me.vaperion.blade.context.BladeContext;
import org.jetbrains.annotations.NotNull;

public interface UsageMessage {
    void sendTo(@NotNull BladeContext context);
    @NotNull String toString();
}
