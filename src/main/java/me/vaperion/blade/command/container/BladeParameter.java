package me.vaperion.blade.command.container;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.annotation.Flag;
import me.vaperion.blade.command.annotation.Optional;
import me.vaperion.blade.command.annotation.Range;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public abstract class BladeParameter {
    protected final String name;
    protected final Class<?> type;
    protected final Optional optional;
    protected final Range range;
    protected final boolean combined;

    public boolean isOptional() {
        return optional != null;
    }

    public String getDefault() {
        return isOptional() ? optional.value() : null;
    }

    public boolean hasRange() {
        return range != null;
    }

    public boolean allowsNull() {
        if (!isOptional()) return false;
        return optional.value().equalsIgnoreCase("null") || optional.value().equals("") || optional.value().equals("none");
    }

    public static class CommandParameter extends BladeParameter {
        public CommandParameter(String name, Class<?> type, Optional optional, Range range, boolean combined) {
            super(name, type, optional, range, combined);
        }
    }

    public static class FlagParameter extends BladeParameter {
        @Getter private final Flag flag;

        public FlagParameter(String name, Class<?> type, Flag flag) {
            super(name, type, null, null, false);

            this.flag = flag;
        }

        public boolean isBooleanFlag() {
            return this.type == boolean.class;
        }

        public String extractFrom(Map<Character, String> flagMap) {
            if (!flagMap.containsKey(flag.value())) return isBooleanFlag() ? "false" : null;
            return flagMap.get(flag.value());
        }
    }
}
