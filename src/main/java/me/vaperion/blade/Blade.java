package me.vaperion.blade;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.argument.ProviderAnnotation;
import me.vaperion.blade.command.bindings.Binding;
import me.vaperion.blade.command.container.ContainerCreator;
import me.vaperion.blade.command.help.HelpGenerator;
import me.vaperion.blade.command.service.BladeCommandService;
import me.vaperion.blade.completer.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
@Getter
@Builder(builderMethodName = "of")
public class Blade {
    private final BladeCommandService commandService = new BladeCommandService();
    private final boolean overrideCommands, overrideBladeCommands;
    private final String fallbackPrefix;
    private final ContainerCreator<?> containerCreator;
    private final TabCompleter tabCompleter;
    private final HelpGenerator helpGenerator;
    private final Consumer<Runnable> asyncExecutor;

    @Singular("bind0") private final Map<Map.Entry<Class<?>, Class<? extends ProviderAnnotation>>, BladeProvider<?>> customProviderMap;
    @Singular private final List<Binding> bindings;

    private void register(@Nullable Object instance, @NotNull Class<?> clazz) {
        commandService.getCommandRegistrar().registerClass(instance, clazz);
    }

    @NotNull
    public Blade register(@NotNull Class<?> containerClass) {
        register(null, containerClass);
        return this;
    }

    @NotNull
    public Blade register(@NotNull Object containerInstance) {
        register(containerInstance, containerInstance.getClass());
        return this;
    }

    public static BladeBuilder of() {
        return new BladeBuilder() {
            @Override
            public Blade build() {
                Blade blade = super.build();

                blade.commandService.setOverrideCommands(blade.overrideCommands);
                blade.commandService.setOverrideBladeCommands(blade.overrideBladeCommands);

                if (blade.containerCreator == null)
                    throw new NullPointerException();
                else
                    blade.commandService.setContainerCreator(blade.containerCreator);

                if (blade.fallbackPrefix != null)
                    blade.commandService.setFallbackPrefix(blade.fallbackPrefix);

                if (blade.tabCompleter != null)
                    blade.commandService.setTabCompleter(blade.tabCompleter);

                if (blade.helpGenerator != null)
                    blade.commandService.setHelpGenerator(blade.helpGenerator);

                if (blade.asyncExecutor != null)
                    blade.commandService.setAsyncExecutor(blade.asyncExecutor);

                for (Binding binding : blade.bindings) {
                    binding.bind(blade.commandService);
                }

                blade.commandService.getTabCompleter().init(blade.commandService);

                for (Map.Entry<Map.Entry<Class<?>, Class<? extends ProviderAnnotation>>, BladeProvider<?>> entry : blade.customProviderMap.entrySet()) {
                    //noinspection deprecation
                    blade.commandService.bindProviderUnsafely(entry.getKey().getKey(), entry.getValue(), entry.getKey().getValue());
                }

                return blade;
            }
        };
    }

    public static class BladeBuilder {
        public <T> BladeBuilder bind(Class<T> clazz, BladeProvider<T> provider) {
            bind0(new AbstractMap.SimpleEntry<>(clazz, null), provider);
            return this;
        }

        public <T> BladeBuilder bind(Class<T> clazz, BladeProvider<T> provider, Class<? extends ProviderAnnotation> annotation) {
            bind0(new AbstractMap.SimpleEntry<>(clazz, annotation), provider);
            return this;
        }
    }
}
