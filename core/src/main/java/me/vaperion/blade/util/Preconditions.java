package me.vaperion.blade.util;

import java.util.function.Function;

public final class Preconditions {

    private Preconditions() {}

    public static boolean checkNotNull(Object object, String message) {
        if (object == null) throw new NullPointerException(message);
        return true;
    }

    public static boolean checkState(boolean expression, String message) {
        if (!expression) throw new IllegalStateException(message);
        return true;
    }

    public static String checkNotEmpty(String string, String replacement) {
        if (string == null || string.isEmpty()) return replacement;
        return string;
    }

    public static <O, T> T runOrDefault(O object, T defaultValue, Function<O, T> function) {
        if (object == null) return defaultValue;
        return function.apply(object);
    }

}
