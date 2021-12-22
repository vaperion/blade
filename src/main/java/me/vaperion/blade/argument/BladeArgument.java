package me.vaperion.blade.argument;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.vaperion.blade.command.BladeParameter;

@Getter
@RequiredArgsConstructor
public class BladeArgument {

    private final BladeParameter parameter;

    @Setter
    private Type type;
    @Setter
    private String string;

    public enum Type {
        PROVIDED,
        OPTIONAL
    }

}
