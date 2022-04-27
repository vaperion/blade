package me.vaperion.blade.context;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public final class Context {

    private final Blade blade;

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
        return arguments;
    }

    @Nullable
    public String argument(int index) {
        if (index < 0 || index >= arguments.length) return null;
        return arguments[index];
    }

    @NotNull
    public String alias() {
        return alias;
    }

    @NotNull
    public WrappedSender<?> sender() {
        return sender;
    }

    @NotNull
    public Blade blade() {
        return blade;
    }

}
