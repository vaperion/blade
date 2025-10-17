package me.vaperion.blade.tokenizer.input.token.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.tokenizer.input.token.Token;
import me.vaperion.blade.tokenizer.input.token.flag.FlagValue;
import me.vaperion.blade.tokenizer.input.token.flag.ImplicitFlag;
import me.vaperion.blade.tokenizer.input.token.flag.ProvidedFlag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@Getter
@RequiredArgsConstructor
public final class FlagToken implements Token {
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    private final Map<Character, FlagValue> flags;

    @Override
    public String toString() {
        return "Flags(" + flags + ")";
    }

    public static class Builder {
        private final Map<Character, FlagValue> flags = new HashMap<>();

        @ApiStatus.Internal
        public boolean contains(char c) {
            return flags.containsKey(c);
        }

        @NotNull
        @Contract("_ -> this")
        public Builder addImplicit(char c) {
            flags.put(c, new ImplicitFlag());
            return this;
        }

        @NotNull
        @Contract("_, _ -> this")
        public Builder add(char c, @NotNull String value) {
            flags.put(c, new ProvidedFlag(value));
            return this;
        }

        @NotNull
        @Contract("-> new")
        public FlagToken build() {
            return new FlagToken(flags);
        }
    }
}
