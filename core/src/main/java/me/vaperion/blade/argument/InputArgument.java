package me.vaperion.blade.argument;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.vaperion.blade.annotation.api.Forwarded;
import me.vaperion.blade.annotation.parameter.Opt;
import me.vaperion.blade.annotation.parameter.Range;
import me.vaperion.blade.command.BladeParameter;
import me.vaperion.blade.exception.BladeParseError;
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
public final class InputArgument {

    private final BladeParameter parameter;

    @Setter(onMethod_ = @ApiStatus.Internal)
    @Nullable
    private String value;
    @Setter(onMethod_ = @ApiStatus.Internal)
    private Status status;

    private final List<String> data = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    private final Map<Class<? extends Annotation>, List<Annotation>> annotations = new HashMap<>();

    public InputArgument(@NotNull BladeParameter parameter,
                         @Nullable String value,
                         @NotNull InputArgument.Status status) {
        this.parameter = parameter;
        this.value = value;
        this.status = status;
    }

    /**
     * Gets the value of the argument, throwing an exception if it is not present.
     *
     * @return the value of the argument
     *
     * @throws IllegalStateException if the value is not present
     */
    @NotNull
    public String requireValue() {
        if (value == null) {
            throw BladeParseError.recoverable("A value must be specified for the '" +
                parameter.name() + "' argument.");
        }

        return value;
    }

    /**
     * Gets the {@link Opt} annotation of the argument, if present.
     *
     * @return the Opt annotation, or null if not present
     */
    @Nullable
    public Opt optional() {
        return parameter.optional();
    }

    /**
     * Checks if the argument is optional with a specific type.
     *
     * @param type the type to check
     * @return true if the argument is optional with the specified type, false otherwise
     */
    public boolean isOptionalWithType(@NotNull Opt.Type type) {
        Opt opt = optional();
        return opt != null && opt.value() == type;
    }

    /**
     * Checks if the argument is optional and accepts null values.
     *
     * @return true if the argument is optional and accepts null values, false otherwise
     */
    @SuppressWarnings("RedundantIfStatement")
    public boolean isOptionalAcceptingNull() {
        Opt opt = optional();
        if (opt == null) return false;

        if (opt.value() == Opt.Type.EMPTY_OR_CUSTOM)
            return opt.custom().isEmpty();
        else if (opt.value() == Opt.Type.EMPTY || opt.value() == Opt.Type.SENDER)
            return true;

        return false;
    }

    /**
     * Gets the {@link Range} annotation of the argument, if present.
     *
     * @return the Range annotation, or null if not present
     */
    @Nullable
    public Range range() {
        return parameter.range();
    }

    /**
     * Gets a {@link Forwarded} from the argument.
     *
     * @param annotationClass the class of the annotation to get
     * @param <T>             the type of the annotation
     * @return the annotation if present, otherwise null
     */
    @Nullable
    public <T extends Annotation> T annotation(@NotNull Class<T> annotationClass) {
        List<T> all = annotationList(annotationClass);
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * Gets all {@link Forwarded}s of a specific type from the argument.
     *
     * @param annotationClass the class of the annotation to get
     * @param <T>             the type of the annotation
     * @return a list of annotations of the specified type
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends Annotation> List<T> annotationList(@NotNull Class<T> annotationClass) {
        return (List<T>) annotations.getOrDefault(annotationClass, new ArrayList<>());
    }

    @ApiStatus.Internal
    public void addAnnotations(@NotNull List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            if (!annotation.annotationType().isAnnotationPresent(Forwarded.class))
                continue;

            this.annotations.computeIfAbsent(annotation.annotationType(),
                    k -> new ArrayList<>())
                .add(annotation);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public enum Status {
        /**
         * The argument was provided.
         */
        PRESENT,
        /**
         * The argument was not provided.
         */
        NOT_PRESENT,
        ;

        /**
         * Checks if the argument was provided.
         *
         * @return true if the argument was provided, false otherwise
         */
        public boolean isPresent() {
            return this == PRESENT;
        }
    }

}
