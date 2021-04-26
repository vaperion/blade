package me.vaperion.blade.command.service;

import lombok.Getter;
import lombok.Setter;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.argument.ProviderAnnotation;
import me.vaperion.blade.command.bindings.impl.DefaultBindings;
import me.vaperion.blade.command.container.BladeCommand;
import me.vaperion.blade.command.container.BladeProviderContainer;
import me.vaperion.blade.command.container.ContainerCreator;
import me.vaperion.blade.command.container.ICommandContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BladeCommandService {

    final List<BladeProviderContainer<?>> providers = new LinkedList<>();
    final List<BladeCommand> commands = new LinkedList<>();
    final Map<String, List<BladeCommand>> aliasCommands = new LinkedHashMap<>();
    final Map<String, ICommandContainer> containerMap = new LinkedHashMap<>();

    @Setter @Getter private String fallbackPrefix = null;
    @Setter @Getter private ContainerCreator<?> containerCreator = ContainerCreator.NONE;

    @Getter private final BladeCommandRegistrar commandRegistrar = new BladeCommandRegistrar(this);
    @Getter private final BladeCommandResolver commandResolver = new BladeCommandResolver(this);
    @Getter private final BladeCommandParser commandParser = new BladeCommandParser(this);
    @Getter private final BladeCommandCompleter commandCompleter = new BladeCommandCompleter(this);

    public BladeCommandService() {
        new DefaultBindings().bind(this);
    }

    public <T> void bindProvider(@NotNull Class<T> clazz, @NotNull BladeProvider<T> provider) {
        bindProvider(clazz, provider, null);
    }

    public <T> void bindProvider(@NotNull Class<T> clazz, @NotNull BladeProvider<T> provider, @Nullable Class<? extends ProviderAnnotation> annotation) {
        this.providers.add(new BladeProviderContainer<>(clazz, provider, annotation));
    }

    public void releaseProvider(@NotNull Class<?> clazz) {
        this.providers.removeIf(bladeProviderContainer -> bladeProviderContainer.getType() == clazz);
    }

    @Deprecated
    @SuppressWarnings({"unchecked", "DeprecatedIsStillUsed"})
    public <T> void bindProviderUnsafely(@NotNull Class<T> clazz, @NotNull BladeProvider<?> provider, @Nullable Class<? extends ProviderAnnotation> annotation) {
        this.providers.add(new BladeProviderContainer<>(clazz, (BladeProvider<T>) provider, annotation));
    }

}
