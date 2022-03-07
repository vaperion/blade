package me.vaperion.blade.utils;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public class LoadedValue<T> {

    private T value;
    @Getter private boolean loaded;

    @Nullable
    public T get() {
        if (!loaded) throw new IllegalStateException("Value is not loaded!");
        return value;
    }

    @NotNull
    public T ensureGet() {
        return Objects.requireNonNull(get());
    }

    @Nullable
    public T getOrLoad(@NotNull Supplier<T> supplier) {
        if (!loaded) set(supplier.get());
        return get();
    }

    @NotNull
    public T ensureGetOrLoad(@NotNull Supplier<T> supplier) {
        return Objects.requireNonNull(getOrLoad(supplier));
    }

    public void set(@Nullable T value) {
        this.value = value;
        loaded = true;
    }

}
