package me.vaperion.blade.command;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.annotation.Completer;
import me.vaperion.blade.annotation.Flag;
import me.vaperion.blade.annotation.Optional;
import me.vaperion.blade.annotation.Range;
import me.vaperion.blade.argument.BladeProvider;
import me.vaperion.blade.exception.BladeExitMessage;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Getter
@RequiredArgsConstructor
public abstract class BladeParameter {

    private static final LoadingCache<Class<?>, BladeProvider<?>> COMPLETER_CACHE = CacheBuilder.newBuilder()
          .build(new CacheLoader<Class<?>, BladeProvider<?>>() {
              @Override
              public BladeProvider<?> load(Class<?> clazz) {
                  try {
                      return (BladeProvider<?>) clazz.newInstance();
                  } catch (Exception ex) {
                      throw new IllegalArgumentException("Provided completer '" + clazz.getSimpleName() + "' does not have an empty constructor.");
                  }
              }
          });

    protected final String name;
    protected final Class<?> type;
    protected final List<String> data;
    protected final Optional optional;
    protected final Range range;
    protected final Completer completer;
    protected final AnnotatedElement element;
    protected final boolean combined;

    public boolean isOptional() {
        return optional != null;
    }

    public boolean hasRange() {
        return range != null;
    }

    public boolean hasCustomTabCompleter() {
        return completer != null;
    }

    public BladeProvider<?> getCompleter() {
        if (!hasCustomTabCompleter()) return null;
        try {
            return COMPLETER_CACHE.get(completer.value());
        } catch (ExecutionException e) {
            e.getCause().printStackTrace();
            throw new BladeExitMessage("An exception was thrown while attempting to load custom tab completer.");
        }
    }

    public String getDefault() {
        return isOptional() ? optional.value() : null;
    }

    public boolean ignoreFailedArgumentParse() {
        return isOptional() && optional.ignoreFailedArgumentParse();
    }

    public boolean defaultsToNull() {
        if (!isOptional()) return false;
        return "null".equals(optional.value());
    }

    public AnnotatedElement getAnnotatedElement() {
        return element;
    }

    public static class CommandParameter extends BladeParameter {
        public CommandParameter(String name, Class<?> type, List<String> data, Optional optional,
                                Range range, Completer completer, AnnotatedElement element, boolean combined) {
            super(name, type, data, optional, range, completer, element, combined);
        }
    }

    public static class FlagParameter extends BladeParameter {
        @Getter private final Flag flag;

        public FlagParameter(String name, Class<?> type, Optional optional, AnnotatedElement element, Flag flag) {
            super(name, type, Collections.emptyList(), optional, null, null, element, false);

            this.flag = flag;
        }

        public boolean isBooleanFlag() {
            return this.type == boolean.class;
        }

        public String extractFrom(Map<Character, String> flagMap) {
            if (!flagMap.containsKey(flag.value())) {
                if (this.optional != null) return this.optional.value();
                return isBooleanFlag() ? "false" : null;
            }

            return flagMap.get(flag.value());
        }
    }
}
