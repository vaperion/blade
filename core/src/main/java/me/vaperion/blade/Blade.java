package me.vaperion.blade;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.Provider;
import me.vaperion.blade.argument.impl.*;
import me.vaperion.blade.command.Command;
import me.vaperion.blade.container.Container;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.BladePlatform;
import me.vaperion.blade.service.*;
import me.vaperion.blade.util.Binding;
import me.vaperion.blade.util.ClassUtil;
import me.vaperion.blade.util.PermissionPredicate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Consumer;

@Getter
public final class Blade {

    @NotNull
    @Contract("_ -> new")
    public static Builder forPlatform(@NotNull BladePlatform platform) {
        return new Builder(platform);
    }

    private final BladePlatform platform;
    private final BladeConfiguration configuration;

    private final Map<String, PermissionPredicate> permissionPredicates = new HashMap<>();

    private final List<Provider<?>> providers = new ArrayList<>();
    private final List<Command> commands = new ArrayList<>();
    private final Map<String, List<Command>> aliasToCommands = new HashMap<>();
    private final Map<String, Container> containers = new HashMap<>();

    private final CommandRegistrar registrar = new CommandRegistrar(this);
    private final CommandResolver resolver = new CommandResolver(this);
    private final CommandParser parser = new CommandParser(this);
    private final CommandCompleter completer = new CommandCompleter(this);
    private final PermissionTester permissionTester = new PermissionTester(this);

    private Blade(Builder builder) {
        this.platform = builder.platform;
        this.configuration = builder.configuration;

        permissionPredicates.putAll(builder.permissionPredicates);

        Binder binder = new Binder(builder, true);
        binder.bind(UUID.class, new UUIDArgument());
        binder.bind(String.class, new StringArgument());
        binder.bind(boolean.class, new BooleanArgument());
        binder.bind(Boolean.class, new BooleanArgument());
        binder.bind(int.class, new IntArgument());
        binder.bind(Integer.class, new IntArgument());
        binder.bind(long.class, new LongArgument());
        binder.bind(Long.class, new LongArgument());
        binder.bind(double.class, new DoubleArgument());
        binder.bind(Double.class, new DoubleArgument());
        binder.bind(float.class, new FloatArgument());
        binder.bind(Float.class, new FloatArgument());
        binder.bind(Enum.class, new EnumArgument());

        for (Binding<?> binding : builder.bindings) {
            if (binding instanceof Binding.Release) {
                providers.removeIf(provider -> binding.getType() == provider.getType() && provider.doAnnotationsMatch(binding.getAnnotations()));
            } else {
                providers.add(Provider.unsafe(binding.getType(), binding.getProvider(), binding.getAnnotations()));
            }
        }

        configuration.getTabCompleter().init(this);
    }

    @NotNull
    @Contract("_, _ -> this")
    public Blade register(@Nullable Object instance, @NotNull Class<?> clazz) {
        registrar.registerClass(instance, clazz);
        return this;
    }

    @NotNull
    @Contract("_ -> this")
    public Blade register(@NotNull Class<?> clazz) {
        registrar.registerClass(null, clazz);
        return this;
    }

    @NotNull
    @Contract("_ -> this")
    public Blade register(@NotNull Object instance) {
        registrar.registerClass(instance, instance.getClass());
        return this;
    }

    @NotNull
    @Contract("_, _ -> this")
    public Blade registerPackage(@NotNull Class<?> clazz, @NotNull String packageName) {
        ClassUtil.getClassesInPackage(clazz, packageName).forEach(this::register);
        return this;
    }

    public static final class Builder {
        private final BladePlatform platform;
        private final BladeConfiguration configuration;

        private final Map<String, PermissionPredicate> permissionPredicates = new HashMap<>();
        private final List<Binding<?>> bindings = new ArrayList<>();

        private Builder(BladePlatform platform) {
            this.platform = platform;
            this.configuration = new BladeConfiguration();
            platform.configureBlade(this, this.configuration);
        }

        @NotNull
        @Contract("_ -> this")
        public Builder config(@NotNull Consumer<BladeConfiguration> consumer) {
            consumer.accept(configuration);
            configuration.validate();
            return this;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder bind(@NotNull Consumer<Binder> consumer) {
            Binder binder = new Binder(this, false);
            consumer.accept(binder);
            return this;
        }

        @NotNull
        @Contract("_ -> this")
        public Builder permission(@NotNull Consumer<PredicateAdder> consumer) {
            PredicateAdder adder = new PredicateAdder(this);
            consumer.accept(adder);
            return this;
        }

        @NotNull
        @Contract(" -> new")
        public Blade build() {
            return new Blade(this);
        }

        @RequiredArgsConstructor
        public static final class Binder {
            private final Builder builder;
            private final boolean insertToBeginning;

            @SafeVarargs
            public final <T> void bind(@NotNull Class<T> type, @NotNull ArgumentProvider<T> provider, @NotNull Class<? extends Annotation>... annotations) {
                bind(type, provider, Arrays.asList(annotations));
            }

            public <T> void bind(@NotNull Class<T> type, @NotNull ArgumentProvider<T> provider, @NotNull List<Class<? extends Annotation>> annotations) {
                Binding<T> binding = new Binding<>(type, provider, annotations);
                if (insertToBeginning) {
                    builder.bindings.add(0, binding);
                } else {
                    builder.bindings.add(binding);
                }
            }

            @SafeVarargs
            public final <T> void unsafeBind(@NotNull Class<T> type, @NotNull ArgumentProvider<?> provider, @NotNull Class<? extends Annotation>... annotations) {
                unsafeBind(type, provider, Arrays.asList(annotations));
            }

            public <T> void unsafeBind(@NotNull Class<T> type, @NotNull ArgumentProvider<?> provider, @NotNull List<Class<? extends Annotation>> annotations) {
                Binding<?> binding = Binding.unsafe(type, provider, annotations);
                if (insertToBeginning) {
                    builder.bindings.add(0, binding);
                } else {
                    builder.bindings.add(binding);
                }
            }

            @SafeVarargs
            public final <T> void release(@NotNull Class<T> type, @NotNull Class<? extends Annotation>... annotations) {
                release(type, Arrays.asList(annotations));
            }

            public <T> void release(@NotNull Class<T> type, @NotNull List<Class<? extends Annotation>> annotations) {
                Binding<?> binding = Binding.release(type, annotations);
                if (insertToBeginning) {
                    builder.bindings.add(0, binding);
                } else {
                    builder.bindings.add(binding);
                }
            }
        }

        @RequiredArgsConstructor
        public static final class PredicateAdder {
            private final Builder builder;

            public void predicate(@NotNull String permission, @NotNull PermissionPredicate predicate) {
                builder.permissionPredicates.put(permission, predicate);
            }
        }
    }

}
