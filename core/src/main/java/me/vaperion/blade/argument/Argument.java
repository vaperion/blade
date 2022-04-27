package me.vaperion.blade.argument;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.vaperion.blade.command.Parameter;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public final class Argument {

    private final Parameter parameter;

    @Setter private Type type;
    @Setter private String string;
    private final List<String> data = new ArrayList<>();

    public enum Type {
        PROVIDED,
        OPTIONAL
    }

}
