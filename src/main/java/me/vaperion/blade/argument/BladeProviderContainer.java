package me.vaperion.blade.argument;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BladeProviderContainer<T> {
    private final Class<T> type;
    private final BladeProvider<T> provider;
    private final Class<? extends ProviderAnnotation> requiredAnnotation;
}
