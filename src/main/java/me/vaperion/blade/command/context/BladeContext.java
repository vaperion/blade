package me.vaperion.blade.command.context;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class BladeContext {

    private final WrappedSender<?> sender;
    private final String alias;
    private final String[] arguments;

    public void reply(@NotNull String message) {
        sender.sendMessage(message);
    }

    public void reply(@NotNull String... message) {
        sender.sendMessage(message);
    }

    @NotNull
    public String[] arguments() {
        return this.arguments;
    }

    @Nullable
    public String argument(int index) {
        if (index < 0 || index >= arguments.length) return null;
        return arguments[index];
    }

    @NotNull
    public String alias() {
        return this.alias;
    }

    @NotNull
    public WrappedSender<?> sender() {
        return this.sender;
    }

}
