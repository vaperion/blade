package me.vaperion.blade.argument.internal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.argument.ArgumentProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;

@ApiStatus.Internal
@SuppressWarnings({ "rawtypes", "unchecked" })
@Getter
@RequiredArgsConstructor
public class ArgBinding<T> {
    @ApiStatus.Internal
    @NotNull
    @Contract("_, _, _ -> new")
    public static ArgBinding<?> unsafe(@NotNull Class<?> type,
                                       @NotNull ArgumentProvider<?> provider,
                                       @NotNull List<Class<? extends Annotation>> annotations) {
        return new ArgBinding(type, provider, annotations);
    }

    @ApiStatus.Internal
    @NotNull
    @Contract("_, _ -> new")
    public static ArgBinding<?> release(@NotNull Class<?> type,
                                        @NotNull List<Class<? extends Annotation>> annotations) {
        return new Release<>(type, null, annotations);
    }

    private final Class<T> type;
    private final ArgumentProvider<T> provider;
    private final List<Class<? extends Annotation>> annotations;

    public static final class Release<T> extends ArgBinding<T> {
        public Release(@NotNull Class<T> type,
                       @Nullable ArgumentProvider<T> provider,
                       @NotNull List<Class<? extends Annotation>> annotations) {
            super(type, provider, annotations);
        }
    }
}
