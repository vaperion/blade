package me.vaperion.blade.command.argument;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.argument.ProviderAnnotation;

@Getter
@RequiredArgsConstructor
public class BladeProviderContainer<T> {
    private final Class<T> type;
    private final BladeProvider<T> provider;
    private final Class<? extends ProviderAnnotation> requiredAnnotation;
}
