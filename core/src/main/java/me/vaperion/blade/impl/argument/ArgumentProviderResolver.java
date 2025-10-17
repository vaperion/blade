package me.vaperion.blade.impl.argument;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.api.Forwarded;
import me.vaperion.blade.annotation.command.*;
import me.vaperion.blade.annotation.parameter.*;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.internal.ArgProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ArgumentProviderResolver {

    private static final List<Class<? extends Annotation>> INTERNAL_ANNOTATIONS = Arrays.asList(
        Provider.class, Data.class, Flag.class, Name.class,
        Opt.class, Range.class, Sender.class, Greedy.class,

        Async.class, Command.class, Description.class, ExtraUsage.class, Hidden.class,
        Quoted.class, Permission.class, Usage.class, MainLabel.class
    );

    private final Blade blade;

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> ArgumentProvider<T> resolveRecursively(@NotNull Class<T> clazz,
                                                      @NotNull List<Annotation> annotations) {
        Class<?> parent = clazz;
        do {
            ArgumentProvider<T> provider = (ArgumentProvider<T>) resolve(parent, annotations);
            if (provider != null) return provider;

            parent = parent.getSuperclass();
        } while (parent != Object.class && parent != null);

        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> ArgumentProvider<T> resolve(@NotNull Class<T> clazz,
                                            @NotNull List<Annotation> annotations) {

        List<Class<? extends Annotation>> inputAnnotations = annotations.stream()
            .map(Annotation::annotationType)
            .filter(c -> !c.isAnnotationPresent(Forwarded.class))
            .map(c -> (Class<? extends Annotation>) c)
            .collect(Collectors.toList());

        inputAnnotations.removeIf(INTERNAL_ANNOTATIONS::contains);

        return blade.providers().stream()
            .filter(container -> container.type() == clazz)
            .filter(container -> container.doAnnotationsMatch(inputAnnotations))
            .limit(1)
            .map(ArgProvider::provider)
            .map(provider -> (ArgumentProvider<T>) provider)
            .findFirst().orElse(null);
    }

}