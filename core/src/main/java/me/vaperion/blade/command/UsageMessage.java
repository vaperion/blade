package me.vaperion.blade.command;

import me.vaperion.blade.context.Context;
import org.jetbrains.annotations.NotNull;

public interface UsageMessage {
    void sendTo(@NotNull Context context);

    @NotNull String toString();
}