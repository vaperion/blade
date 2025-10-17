package me.vaperion.blade.tokenizer.input.token.flag;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ImplicitFlag implements FlagValue {
    @Override
    public @NotNull String value() {
        return "true";
    }

    @Override
    public String toString() {
        return "Implicit";
    }
}