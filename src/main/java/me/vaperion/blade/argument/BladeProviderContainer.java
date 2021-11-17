package me.vaperion.blade.argument;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class BladeProviderContainer<T> {
    private final Class<T> type;
    private final BladeProvider<T> provider;
    private final List<Class<? extends ProviderAnnotation>> requiredAnnotations;
}
