package me.vaperion.blade.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.annotation.argument.Completer;
import me.vaperion.blade.annotation.argument.Flag;
import me.vaperion.blade.annotation.argument.Optional;
import me.vaperion.blade.annotation.argument.Range;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.exception.BladeExitMessage;
import org.jetbrains.annotations.Nullable;

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
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception e) {
            if (e.getCause() != null) e.getCause().printStackTrace();
            else e.printStackTrace();
            throw new BladeExitMessage("An exception was thrown while attempting to load the custom tab completer.");
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
        public CommandParameter(String name, Class<?> type, List<String> data, Optional optional, Range range, Completer completer, boolean text, AnnotatedElement element) {
            super(name, type, data, optional, range, completer, text, element);
        }
    }

    @Getter
    public static final class FlagParameter extends Parameter {
        private final Flag flag;

        public FlagParameter(String name, Class<?> type, Optional optional, AnnotatedElement element, Flag flag) {
            super(name, type, Collections.emptyList(), optional, null, null, false, element);
            this.flag = flag;
        }

        public boolean isBooleanFlag() {
            return this.type == boolean.class;
        }

        @Nullable
        public String extractFrom(Map<Character, String> flagMap) {
            if (!flagMap.containsKey(flag.value())) {
                if (this.optional != null) return this.optional.value();
                return isBooleanFlag() ? "false" : null;
            }

            return flagMap.get(flag.value());
        }
    }

}
