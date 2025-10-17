package me.vaperion.blade.tokenizer.input.token.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.tokenizer.input.token.Token;

@SuppressWarnings("unused")
@Getter
@RequiredArgsConstructor
public final class ArgumentToken implements Token {
    private final String value;

    @Override
    public String toString() {
        return "Argument(`" + value + "`)";
    }
}
