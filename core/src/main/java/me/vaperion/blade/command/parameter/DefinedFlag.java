package me.vaperion.blade.command.parameter;

import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.command.BladeParameter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;

@Getter
@ApiStatus.Internal
public final class DefinedFlag extends BladeParameter {
    private final Flag flag;

    public DefinedFlag(@NotNull Blade blade,
                       @NotNull String name,
                       @NotNull Class<?> type,
                       @NotNull AnnotatedElement element,
                       @NotNull Flag flag) {
        super(blade, name, type, Collections.emptyList(), element);

        this.flag = flag;
    }

    public char getChar() {
        return this.flag.value();
    }

    @NotNull
    public String getLongName() {
        return this.flag.longName();
    }

    public boolean isBooleanFlag() {
        return this.type == boolean.class;
    }
}
