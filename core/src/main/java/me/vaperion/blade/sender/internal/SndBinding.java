package me.vaperion.blade.sender.internal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.sender.SenderProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
@SuppressWarnings({ "rawtypes", "unchecked" })
@Getter
@RequiredArgsConstructor
public class SndBinding<T> {
    @ApiStatus.Internal
    @NotNull
    @Contract("_, _ -> new")
    public static SndBinding<?> unsafe(@NotNull Class<?> type,
                                       @NotNull SenderProvider<?> provider) {
        return new SndBinding(type, provider);
    }

    @ApiStatus.Internal
    @NotNull
    @Contract("_ -> new")
    public static SndBinding<?> release(@NotNull Class<?> type) {
        return new Release<>(type, null);
    }

    private final Class<T> type;
    private final SenderProvider<T> provider;

    public static final class Release<T> extends SndBinding<T> {
        public Release(@NotNull Class<T> type,
                       @Nullable SenderProvider<T> provider) {
            super(type, provider);
        }
    }
}
