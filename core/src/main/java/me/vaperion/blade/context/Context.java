package me.vaperion.blade.context;

import lombok.AllArgsConstructor;
import lombok.Setter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.command.BladeParameter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;

@SuppressWarnings("unused")
@AllArgsConstructor
public final class Context {

    private final Blade blade;

    private final Sender<?> sender;
    @Setter(onMethod_ = @ApiStatus.Internal)
    private String label;
    private final String[] arguments;

    @NotNull
    public String[] arguments() {
        return arguments;
    }

    @Nullable
    public String argument(int index) {
        if (index < 0 || index >= arguments.length) return null;
        return arguments[index];
    }

    @Nullable
    public <T> T parseArgument(int index, @NotNull Class<T> argumentClass) {
        return parseArgument(index, argumentClass, "");
    }

    @Nullable
    public <T> T parseArgument(int index, @NotNull Class<T> argumentClass, @NotNull String defaultValue) {
        ArgumentProvider<T> provider = blade.providerResolver().resolveRecursively(argumentClass, Collections.emptyList());

        if (provider == null)
            throw new IllegalArgumentException("No provider found for " + argumentClass.getName());

        return parseArgument(index, argumentClass, provider, defaultValue);
    }

    @Nullable
    public <T> T parseArgument(int index, @NotNull ArgumentProvider<T> provider) {
        return parseArgument(index, provider, "");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T parseArgument(int index, @NotNull ArgumentProvider<T> provider, @NotNull String defaultValue) {
        return parseArgument(index,
            (Class<T>) ((ParameterizedType) provider.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0],
            provider,
            defaultValue);
    }

    @Nullable
    public <T> T parseArgument(int index, @NotNull Class<T> classOfT,
                               @NotNull ArgumentProvider<T> provider, @NotNull String defaultValue) {
        InputArgument arg = new InputArgument(new BladeParameter(
            blade,
            /*name*/ "argument " + (index + 1),
            /*type*/ classOfT,
            /*data*/ Collections.emptyList(),
            /*element*/ null
        ));

        String provided = argument(index);
        if (provided == null) {
            arg.status(InputArgument.Status.NOT_PRESENT);
            arg.value(defaultValue);
        } else {
            arg.status(InputArgument.Status.PRESENT);
            arg.value(provided);
        }

        return provider.provide(this, arg);
    }

    /**
     * The label that was used to invoke the command.
     *
     * @return the label
     */
    @NotNull
    public String label() {
        return label;
    }

    /**
     * The sender of the command.
     *
     * @return the sender
     */
    @NotNull
    public Sender<?> sender() {
        return sender;
    }

    /**
     * The Blade instance.
     *
     * @return the blade instance
     */
    @NotNull
    public Blade blade() {
        return blade;
    }

}
