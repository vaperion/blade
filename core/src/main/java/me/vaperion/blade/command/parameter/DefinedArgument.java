package me.vaperion.blade.command.parameter;

import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeParameter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.List;

@ApiStatus.Internal
public final class DefinedArgument extends BladeParameter {
    public DefinedArgument(@NotNull Blade blade,
                           @NotNull String name,
                           @NotNull Class<?> type,
                           @NotNull List<String> data,
                           @NotNull AnnotatedElement element) {
        super(blade, name, type, data, element);
    }
}
