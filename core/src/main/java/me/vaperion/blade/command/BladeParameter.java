package me.vaperion.blade.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.command.Quoted;
import me.vaperion.blade.annotation.parameter.Greedy;
import me.vaperion.blade.annotation.parameter.Opt;
import me.vaperion.blade.annotation.parameter.Provider;
import me.vaperion.blade.annotation.parameter.Range;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.exception.internal.BladeFatalError;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public class BladeParameter {

    private static final Map<Class<? extends ArgumentProvider<?>>, ArgumentProvider<?>>
        PROVIDER_CACHE = new ConcurrentHashMap<>();

    private final Blade blade;

    protected final String name;
    protected final Class<?> type;
    protected final List<String> data;

    @Nullable
    protected final AnnotatedElement element;

    @Getter(AccessLevel.NONE)
    protected boolean alwaysQuoted = false;

    /**
     * Returns all annotations present on this parameter.
     *
     * @return a list of annotations
     */
    @NotNull
    public List<Annotation> annotations() {
        if (element != null) {
            return Arrays.asList(element.getAnnotations());
        }

        return Collections.emptyList();
    }

    /**
     * Returns the {@link Opt} annotation of this parameter, if present.
     *
     * @return the optional annotation, or null if not present
     */
    @Nullable
    public Opt optional() {
        if (element != null) {
            return element.getAnnotation(Opt.class);
        }

        return null;
    }

    /**
     * Returns whether this parameter has an optional annotation.
     *
     * @return true if the parameter has an optional annotation
     */
    public boolean isOptional() {
        return optional() != null;
    }

    /**
     * Returns the {@link Range} annotation of this parameter, if present.
     *
     * @return the range annotation, or null if not present
     */
    @Nullable
    public Range range() {
        if (element != null) {
            return element.getAnnotation(Range.class);
        }

        return null;
    }

    /**
     * Returns whether this parameter has a range annotation.
     *
     * @return true if the parameter has a range annotation
     */
    public boolean hasRange() {
        return range() != null;
    }

    /**
     * Returns whether this parameter has a quoted annotation.
     *
     * @return true if the parameter has a quoted annotation
     */
    public boolean isQuoted() {
        if (alwaysQuoted)
            return true;

        if (element != null) {
            return element.isAnnotationPresent(Quoted.class);
        }

        return false;
    }

    /**
     * Returns the {@link Provider} annotation of this parameter, if present.
     *
     * @return the provider annotation, or null if not present
     */
    @Nullable
    public Provider provider() {
        if (element != null) {
            return element.getAnnotation(Provider.class);
        }

        return null;
    }

    /**
     * Returns whether this parameter has a custom parser.
     *
     * @return true if the parameter has a custom parser
     */
    public boolean hasCustomParser() {
        Provider provider = provider();

        return provider != null && provider.scope().affectsParser();
    }

    /**
     * Returns the custom {@link ArgumentProvider} used for parsing this parameter, if any.
     *
     * @return the custom argument provider, or null if not present
     */
    @Nullable
    public ArgumentProvider<?> customParser() {
        Provider provider = provider();

        if (provider == null || !provider.scope().affectsParser())
            return null;

        return createArgumentProvider(provider.value());
    }

    /**
     * Returns whether this parameter has a custom completer.
     *
     * @return true if the parameter has a custom completer
     */
    public boolean hasCustomCompleter() {
        Provider provider = provider();

        return provider != null && provider.scope().affectsSuggestions();
    }

    /**
     * Returns the custom {@link ArgumentProvider} used for tab-completing this parameter, if any.
     *
     * @return the custom argument provider, or null if not present
     */
    @Nullable
    public ArgumentProvider<?> customCompleter() {
        Provider provider = provider();

        if (provider == null || !provider.scope().affectsSuggestions())
            return null;

        return createArgumentProvider(provider.value());
    }

    /**
     * Returns whether this parameter has a greedy annotation.
     *
     * @return true if the parameter has a greedy annotation
     */
    public boolean isGreedy() {
        return element != null && element.isAnnotationPresent(Greedy.class);
    }

    @ApiStatus.Internal
    @NotNull
    private ArgumentProvider<?> createArgumentProvider(Class<? extends ArgumentProvider<?>> type) {
        try {
            return PROVIDER_CACHE.computeIfAbsent(type,
                c -> {
                    try {
                        return c.newInstance();
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
        } catch (Throwable t) {
            blade.logger().error(
                t.getCause() != null ? t.getCause() : t,
                "An error occurred while attempting to instantiate the argument provider `%s`.",
                type.getCanonicalName()
            );

            throw new BladeFatalError(
                "Failed to load the provider for this argument."
            );
        }
    }

}
