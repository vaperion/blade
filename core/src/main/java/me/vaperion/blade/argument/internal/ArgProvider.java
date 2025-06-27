package me.vaperion.blade.argument.internal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.argument.ArgumentProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;

@ApiStatus.Internal
@SuppressWarnings({ "rawtypes", "unchecked" })
@Getter
@RequiredArgsConstructor
public class ArgProvider<T> {

    @ApiStatus.Internal
    @NotNull
    @Contract("_, _, _ -> new")
    public static ArgProvider<?> unsafe(@NotNull Class<?> type,
                                        @NotNull ArgumentProvider<?> provider,
                                        @NotNull List<Class<? extends Annotation>> requiredAnnotations) {
        return new ArgProvider(type, provider, requiredAnnotations);
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
