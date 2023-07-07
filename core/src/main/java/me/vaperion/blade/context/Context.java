package me.vaperion.blade.context;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.argument.Optional;
import me.vaperion.blade.argument.Argument;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.command.Parameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;

@RequiredArgsConstructor
public final class Context {

    private static final Optional PARSE_OPTIONAL_ARG = new Optional() {
        @Override
        public String value() {
            return "null"; // This is not used inside argument providers, so we can set it statically here.
        }

        @Override
        public boolean ignoreFailedArgumentParse() {
            return false;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Optional.class;
        }
    };

    private final Blade blade;

    private final WrappedSender<?> sender;
    private final String alias;
    private final String[] arguments;

    public void reply(@NotNull String message) {
        sender.sendMessage(message);
    }

    public void reply(@NotNull String... message) {
        sender.sendMessage(message);
    }

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
    public <T> T parseArgument(int index, Class<T> argumentClass) {
        return parseArgument(index, argumentClass, "");
    }

    @Nullable
    public <T> T parseArgument(int index, Class<T> argumentClass, String defaultValue) {
        ArgumentProvider<T> provider = blade.getResolver().recursiveResolveProvider(argumentClass, Collections.emptyList());

        if (provider == null)
            throw new IllegalArgumentException("No provider found for " + argumentClass.getName());

        return parseArgument(index, provider, defaultValue);
    }

    @Nullable
    public <T> T parseArgument(int index, ArgumentProvider<T> provider) {
        return parseArgument(index, provider, "");
    }

    @Nullable
    public <T> T parseArgument(int index, ArgumentProvider<T> provider, String defaultValue) {
        Argument arg = new Argument(new Parameter(
              /*name*/ "argument " + (index + 1),
              /*type*/ (Class<?>) ((ParameterizedType) provider.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0],
              /*data*/ Collections.emptyList(),
              /*optional annotation*/ PARSE_OPTIONAL_ARG,
              /*range annotation*/ null,
              /*completer annotation*/null,
              /*text?*/ false,
              /*element*/ null
        ));

        String provided = argument(index);
        if (provided == null) {
            arg.setType(Argument.Type.OPTIONAL);
            arg.setString(defaultValue);
        } else {
            arg.setType(Argument.Type.PROVIDED);
            arg.setString(provided);
        }

        return provider.provide(this, arg);
    }

    @NotNull
    public String alias() {
        return alias;
    }

    @NotNull
    public WrappedSender<?> sender() {
        return sender;
    }

    @NotNull
    public Blade blade() {
        return blade;
    }

}
