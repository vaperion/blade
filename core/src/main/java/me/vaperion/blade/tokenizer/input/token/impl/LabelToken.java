package me.vaperion.blade.tokenizer.input.token.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.vaperion.blade.tokenizer.input.token.Token;
import org.jetbrains.annotations.ApiStatus;

@SuppressWarnings("unused")
@Getter
@Setter(onMethod_ = @ApiStatus.Internal)
@AllArgsConstructor
public final class LabelToken implements Token {
    private String name;

    @Override
    public String toString() {
        return "Label(`" + name + "`)";
    }
}
