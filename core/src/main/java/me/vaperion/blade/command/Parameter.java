package me.vaperion.blade.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.annotation.argument.Completer;
import me.vaperion.blade.annotation.argument.Flag;
import me.vaperion.blade.annotation.argument.Optional;
import me.vaperion.blade.annotation.argument.Range;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.log.BladeLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public class Parameter {

    private static final Map<Class<? extends ArgumentProvider<?>>, ArgumentProvider<?>> COMPLETER_CACHE = new ConcurrentHashMap<>();

    final String name;
    final Class<?> type;
    final List<String> data;

    final Optional optional;
    final Range range;
    final Completer completer;

    final boolean text;
    final AnnotatedElement element;

    public boolean isOptional() {
        return optional != null;
    }

    public boolean hasRange() {
        return range != null;
    }

    public boolean hasCustomCompleter() {
        return completer != null;
    }

    @Nullable
    public ArgumentProvider<?> getCustomCompleter() {
        if (!hasCustomCompleter()) return null;
        try {
            return COMPLETER_CACHE.computeIfAbsent(completer.value(), c -> {
                try {
                    return c.newInstance();
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            });
        } catch (Throwable t) {
            BladeLogger.DEFAULT.error(
                t.getCause() != null ? t.getCause() : t,
                "An error occurred while attempting to load the custom tab completer for parameter '%s' of type '%s'.",
                name, type.getCanonicalName()
            );

            throw new BladeExitMessage("An error occurred while attempting to load the custom tab completer.");
        }
    }

    @Nullable
    public String getDefault() {
        return isOptional() ? optional.value() : null;
    }

    public boolean ignoreFailedArgumentParse() {
        return isOptional() && optional.ignoreFailedArgumentParse();
    }

    public boolean defaultsToNull() {
        return isOptional() && "null".equals(optional.value());
    }

    public static final class CommandParameter extends Parameter {
        public CommandParameter(@NotNull String name,
                                @NotNull Class<?> type,
                                @NotNull List<String> data,
                                @Nullable Optional optional,
                                @Nullable Range range,
                                @Nullable Completer completer,
                                boolean text,
                                @NotNull AnnotatedElement element) {
            super(name, type, data, optional, range, completer, text, element);
        }
    }

    @Getter
    public static final class FlagParameter extends Parameter {
        private final Flag flag;

        public FlagParameter(@NotNull String name,
                             @NotNull Class<?> type,
                             @NotNull AnnotatedElement element,
                             @NotNull Flag flag) {
            super(name, type, Collections.emptyList(),
                makeFakeOptionalFlag(flag, type), null, null, false, element);

            this.flag = flag;
        }

        public boolean isBooleanFlag() {
            return this.type == boolean.class;
        }

        @Nullable
        public String extractFrom(@NotNull Map<Character, String> flagMap) {
            if (!flagMap.containsKey(flag.value())) {
                if (this.optional != null) return this.optional.value();
                return isBooleanFlag() ? "false" : null;
            }

            return flagMap.get(flag.value());
        }

        @Nullable
        private static Optional makeFakeOptionalFlag(@NotNull Flag flag,
                                                     @NotNull Class<?> type) {
            if (flag.required())
                return null;

            return new Optional() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Optional.class;
                }

                @Override
                public @NotNull String value() {
                    return type == boolean.class ? "false" : "null";
                }

                @Override
                public boolean ignoreFailedArgumentParse() {
                    return false;
                }
            };
        }
    }

}
