package me.vaperion.blade;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade.Builder.Binder;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.impl.*;
import me.vaperion.blade.argument.internal.ArgBinding;
import me.vaperion.blade.argument.internal.ArgProvider;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.Container;
import me.vaperion.blade.log.BladeLogger;
import me.vaperion.blade.platform.BladeConfiguration;
import me.vaperion.blade.platform.BladePlatform;
import me.vaperion.blade.sender.SenderProvider;
import me.vaperion.blade.sender.internal.SndBinding;
import me.vaperion.blade.sender.internal.SndProvider;
import me.vaperion.blade.service.*;
import me.vaperion.blade.util.ClassUtil;
import me.vaperion.blade.util.PermissionPredicate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings({ "unused", "UnusedReturnValue" })
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

    private final List<ArgProvider<?>> providers = new ArrayList<>();
    private final List<SndProvider<?>> senderProviders = new ArrayList<>();
    private final List<BladeCommand> commands = new ArrayList<>();
    private final Map<String, List<BladeCommand>> aliasToCommands = new HashMap<>();
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

        for (ArgBinding<?> binding : builder.bindings) {
            if (binding instanceof ArgBinding.Release) {
                providers.removeIf(provider -> binding.getType() == provider.getType() && provider.doAnnotationsMatch(binding.getAnnotations()));
            } else {
                providers.add(ArgProvider.unsafe(binding.getType(), binding.getProvider(), binding.getAnnotations()));
            }
        }

        for (SndBinding<?> binding : builder.senderBindings) {
            if (binding instanceof SndBinding.Release) {
                senderProviders.removeIf(provider -> binding.getType() == provider.getType());
            } else {
                senderProviders.add(SndProvider.unsafe(binding.getType(), binding.getProvider()));
            }
        }

        configuration.getTabCompleter().init(this);
        platform.ingestBlade(this);
    }

    @NotNull
    public BladeLogger logger() {
        return configuration.getLogger();
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
        private final List<ArgBinding<?>> bindings = new ArrayList<>();
        private final List<SndBinding<?>> senderBindings = new ArrayList<>();

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

        /**
         * @deprecated Use {@link #bind(Consumer)} instead.
         */
        @Deprecated
        @NotNull
        @Contract("_ -> this")
        public Builder bindSender(@NotNull Consumer<SenderBinder> consumer) {
            SenderBinder binder = new SenderBinder(this, false);
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

            /**
             * Register a binding for an argument provider.
             *
             * @param type        the type of the argument
             * @param provider    the provider
             * @param annotations the required annotations for the argument
             * @param <T>         the type
             */
            @SafeVarargs
            public final <T> void bind(@NotNull Class<T> type,
                                       @NotNull ArgumentProvider<T> provider,
                                       @NotNull Class<? extends Annotation>... annotations) {
                bind(type, provider, Arrays.asList(annotations));
            }


            /**
             * Register a binding for an argument provider.
             *
             * @param type        the type of the argument
             * @param provider    the provider
             * @param annotations the required annotations for the argument
             * @param <T>         the type
             */
            public <T> void bind(@NotNull Class<T> type,
                                 @NotNull ArgumentProvider<T> provider,
                                 @NotNull List<Class<? extends Annotation>> annotations) {
                ArgBinding<T> binding = new ArgBinding<>(type, provider, annotations);

                if (insertToBeginning) {
                    builder.bindings.add(0, binding);
                } else {
                    builder.bindings.add(binding);
                }
            }

            /**
             * Register a binding for an argument provider without type safety.
             *
             * @param type        the type of the argument
             * @param provider    the unsafe provider
             * @param annotations the required annotations for the argument
             * @param <T>         the type
             */
            @SafeVarargs
            public final <T> void unsafeBind(@NotNull Class<T> type,
                                             @NotNull ArgumentProvider<?> provider,
                                             @NotNull Class<? extends Annotation>... annotations) {
                unsafeBind(type, provider, Arrays.asList(annotations));
            }

            /**
             * Register a binding for an argument provider without type safety.
             *
             * @param type        the type of the argument
             * @param provider    the unsafe provider
             * @param annotations the required annotations for the argument
             * @param <T>         the type
             */
            public <T> void unsafeBind(@NotNull Class<T> type,
                                       @NotNull ArgumentProvider<?> provider,
                                       @NotNull List<Class<? extends Annotation>> annotations) {
                ArgBinding<?> binding = ArgBinding.unsafe(type, provider, annotations);

                if (insertToBeginning) {
                    builder.bindings.add(0, binding);
                } else {
                    builder.bindings.add(binding);
                }
            }

            /**
             * Release a binding for an argument type.
             *
             * @param type        the type of the argument
             * @param annotations the required annotations
             * @param <T>         the type
             */
            @SafeVarargs
            public final <T> void release(@NotNull Class<T> type,
                                          @NotNull Class<? extends Annotation>... annotations) {
                release(type, Arrays.asList(annotations));
            }

            /**
             * Release a binding for an argument type.
             *
             * @param type        the type of the argument
             * @param annotations the required annotations
             * @param <T>         the type
             */
            public <T> void release(@NotNull Class<T> type,
                                    @NotNull List<Class<? extends Annotation>> annotations) {
                ArgBinding<?> binding = ArgBinding.release(type, annotations);
                if (insertToBeginning) {
                    builder.bindings.add(0, binding);
                } else {
                    builder.bindings.add(binding);
                }
            }

            /**
             * Register a binding for a sender provider.
             *
             * @param type     the type of the sender
             * @param provider the provider
             * @param <T>      the type of the sender
             */
            public <T> void bindSender(@NotNull Class<T> type,
                                       @NotNull SenderProvider<T> provider) {
                SndBinding<T> binding = new SndBinding<>(type, provider);

                if (insertToBeginning) {
                    builder.senderBindings.add(0, binding);
                } else {
                    builder.senderBindings.add(binding);
                }
            }

            /**
             * Register a binding for a sender provider without type safety.
             *
             * @param type     the type of the sender
             * @param provider the unsafe provider
             * @param <T>      the type of the sender
             */
            public <T> void unsafeBindSender(@NotNull Class<T> type,
                                             @NotNull SenderProvider<?> provider) {
                SndBinding<?> binding = SndBinding.unsafe(type, provider);

                if (insertToBeginning) {
                    builder.senderBindings.add(0, binding);
                } else {
                    builder.senderBindings.add(binding);
                }
            }

            /**
             * Release a binding for a sender type.
             *
             * @param type the type of the sender
             * @param <T>  the type of the sender
             */
            public <T> void releaseSender(@NotNull Class<T> type) {
                SndBinding<?> binding = SndBinding.release(type);

                if (insertToBeginning) {
                    builder.senderBindings.add(0, binding);
                } else {
                    builder.senderBindings.add(binding);
                }
            }
        }

        @RequiredArgsConstructor
        public static final class SenderBinder {
            private final Builder builder;
            private final boolean insertToBeginning;

            /**
             * @deprecated Use {@link Binder#bindSender(Class, SenderProvider)} instead.
             */
            @Deprecated
            public <T> void bind(@NotNull Class<T> type,
                                 @NotNull SenderProvider<T> provider) {
                SndBinding<T> binding = new SndBinding<>(type, provider);

                if (insertToBeginning) {
                    builder.senderBindings.add(0, binding);
                } else {
                    builder.senderBindings.add(binding);
                }
            }

            /**
             * @deprecated Use {@link Binder#unsafeBindSender(Class, SenderProvider)} instead.
             */
            @Deprecated
            public <T> void unsafeBind(@NotNull Class<T> type,
                                       @NotNull SenderProvider<?> provider) {
                SndBinding<?> binding = SndBinding.unsafe(type, provider);

                if (insertToBeginning) {
                    builder.senderBindings.add(0, binding);
                } else {
                    builder.senderBindings.add(binding);
                }
            }

            /**
             * @deprecated Use {@link Binder#releaseSender(Class)} instead.
             */
            @Deprecated
            public <T> void release(@NotNull Class<T> type) {
                SndBinding<?> binding = SndBinding.release(type);

                if (insertToBeginning) {
                    builder.senderBindings.add(0, binding);
                } else {
                    builder.senderBindings.add(binding);
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
