package me.vaperion.blade.argument;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
@Getter
@RequiredArgsConstructor
public class Provider<T> {

    @NotNull
    @Contract("_, _, _ -> new")
    public static Provider<?> unsafe(@NotNull Class<?> type, @NotNull ArgumentProvider<?> provider, @NotNull List<Class<? extends Annotation>> requiredAnnotations) {
        return new Provider(type, provider, requiredAnnotations);
    }

    private final Class<T> type;
    private final ArgumentProvider<T> provider;
    private final List<Class<? extends Annotation>> requiredAnnotations;

    public boolean doAnnotationsMatch(@Nullable List<Class<? extends Annotation>> annotations) {
        if (annotations == null || requiredAnnotations == null) return false;
        if (annotations.size() != requiredAnnotations.size()) return false;
        return new HashSet<>(requiredAnnotations).containsAll(annotations);
    }

}
