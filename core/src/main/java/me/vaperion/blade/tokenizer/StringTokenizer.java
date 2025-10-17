package me.vaperion.blade.tokenizer;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class StringTokenizer extends AbstractStringTokenizer {

    public StringTokenizer(String string) {
        super(string);
    }

    @Override
    public void setString(@NotNull String string) {
        super.setString(string);
        super.resetCursor();
    }
}
