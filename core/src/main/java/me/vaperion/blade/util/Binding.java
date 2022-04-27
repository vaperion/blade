package me.vaperion.blade.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.argument.ArgumentProvider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
@Getter
@RequiredArgsConstructor
public class Binding<T> {
    @NotNull
    @Contract("_, _, _ -> new")
    public static Binding<?> unsafe(@NotNull Class<?> type, @NotNull ArgumentProvider<?> provider, @NotNull List<Class<? extends Annotation>> annotations) {
        return new Binding(type, provider, annotations);
    }

    @NotNull
    @Contract("_, _ -> new")
    public static Binding<?> release(@NotNull Class<?> type, @NotNull List<Class<? extends Annotation>> annotations) {
        return new Release<>(type, null, annotations);
    }

    private final Class<T> type;
    private final ArgumentProvider<T> provider;
    private final List<Class<? extends Annotation>> annotations;

    public static final class Release<T> extends Binding<T> {
        public Release(Class<T> type, ArgumentProvider<T> provider, List<Class<? extends Annotation>> annotations) {
            super(type, provider, annotations);
        }
    }
}
