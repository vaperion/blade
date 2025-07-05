package me.vaperion.blade.argument;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.vaperion.blade.annotation.argument.ForwardedAnnotation;
import me.vaperion.blade.command.Parameter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Getter
@RequiredArgsConstructor
public final class Argument {

    private final Parameter parameter;

    @Setter private Type type;
    @Setter private String string;

    private final List<String> data = new ArrayList<>();
    private final Map<Class<? extends Annotation>, List<Annotation>> annotations = new HashMap<>();

    /**
     * Gets a {@link ForwardedAnnotation} from the argument.
     *
     * @param annotationClass the class of the annotation to get
     * @param <T>             the type of the annotation
     * @return the annotation if present, otherwise null
     */
    @Nullable
    public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
        List<T> all = getAnnotations(annotationClass);
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * Gets all {@link ForwardedAnnotation}s of a specific type from the argument.
     *
     * @param annotationClass the class of the annotation to get
     * @param <T>             the type of the annotation
     * @return a list of annotations of the specified type
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends Annotation> List<T> getAnnotations(@NotNull Class<T> annotationClass) {
        return (List<T>) annotations.getOrDefault(annotationClass, new ArrayList<>());
    }

    @ApiStatus.Internal
    public void addAnnotations(@NotNull List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            if (!annotation.annotationType().isAnnotationPresent(ForwardedAnnotation.class))
                continue;

            this.annotations.computeIfAbsent(annotation.annotationType(),
                    k -> new ArrayList<>())
                .add(annotation);
        }
    }

    public enum Type {
        /**
         * The argument was provided.
         */
        PROVIDED,
        /**
         * The argument was not provided, and it is marked as optional.
         */
        OPTIONAL
    }

}
