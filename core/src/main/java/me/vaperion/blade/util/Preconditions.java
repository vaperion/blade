package me.vaperion.blade.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings({ "UnusedReturnValue", "unused" })
public final class Preconditions {

    private Preconditions() {
    }

    @NotNull
    public static <T extends Annotation> T mustGetAnnotation(@NotNull AnnotatedElement annotatedElement,
                                                             @NotNull Class<T> annotationClass) {
        return Objects.requireNonNull(annotatedElement.getAnnotation(annotationClass));
    }

    @Contract("null, _ -> fail")
    public static boolean checkNotNull(@UnknownNullability Object object, @NotNull String message) {
        if (object == null)
            throw new NullPointerException(message);

        return true;
    }

    @Contract("false, _ -> fail")
    public static boolean checkState(boolean expression, @NotNull String message) {
        if (!expression)
            throw new IllegalStateException(message);

        return true;
    }

    @NotNull
    public static String checkNotEmpty(@UnknownNullability String string, @NotNull String replacement) {
        if (string == null || string.isEmpty())
            return replacement;

        return string;
    }

    @NotNull
    public static <O, T> T runOrDefault(@UnknownNullability O object,
                                        @NotNull T defaultValue,
                                        @NotNull Function<O, T> function) {
        if (object == null)
            return defaultValue;

        return function.apply(object);
    }

}
