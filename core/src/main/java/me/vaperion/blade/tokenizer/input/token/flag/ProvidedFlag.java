package me.vaperion.blade.tokenizer.input.token.flag;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ProvidedFlag implements FlagValue {
    private final String value;

    public ProvidedFlag(@NotNull String value) {
        this.value = value;
    }

    @Override
    public @NotNull String value() {
        return value;
    }

    @Override
    public String toString() {
        return "`" + value + "`";
    }
}