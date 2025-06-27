package me.vaperion.blade.sender.internal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.sender.SenderProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
@SuppressWarnings({ "rawtypes", "unchecked" })
@Getter
@RequiredArgsConstructor
public class SndProvider<T> {

    @ApiStatus.Internal
    @NotNull
    @Contract("_, _ -> new")
    public static SndProvider<?> unsafe(@NotNull Class<?> type,
                                        @NotNull SenderProvider<?> provider) {
        return new SndProvider(type, provider);
    }

    private final Class<T> type;
    private final SenderProvider<T> provider;

}
