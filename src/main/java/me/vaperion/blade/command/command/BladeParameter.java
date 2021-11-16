package me.vaperion.blade.command.command;

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

    public boolean ignoreFailedArgumentParse() {
        return isOptional() && optional.ignoreFailedArgumentParse();
    }

    public boolean defaultsToNull() {
        if (!isOptional()) return false;
        return "null".equals(optional.value());
    }

    public static class CommandParameter extends BladeParameter {
        public CommandParameter(String name, Class<?> type, Optional optional, Range range, boolean combined) {
            super(name, type, optional, range, combined);
        }
    }

    public static class FlagParameter extends BladeParameter {
        @Getter private final Flag flag;

        public FlagParameter(String name, Class<?> type, Optional optional, Flag flag) {
            super(name, type, optional, null, false);

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
