package me.vaperion.blade.command;

import me.vaperion.blade.context.Context;
import org.jetbrains.annotations.NotNull;

public interface InternalUsage<Text> {
    @NotNull
    Text message();

    void sendTo(@NotNull Context context);
}