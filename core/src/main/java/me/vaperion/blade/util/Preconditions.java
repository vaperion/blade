package me.vaperion.blade.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Preconditions {

    public boolean checkNotNull(Object object, String message) {
        if (object == null) throw new NullPointerException(message);
        return true;
    }

    public boolean checkState(boolean expression, String message) {
        if (!expression) throw new IllegalStateException(message);
        return true;
    }

    public String checkNotEmpty(String string, String replacement) {
        if (string == null || string.isEmpty()) return replacement;
        return string;
    }

}
