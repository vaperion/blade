package me.vaperion.blade.command.service;

import lombok.Getter;
import lombok.Setter;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.argument.BladeProviderContainer;
import me.vaperion.blade.command.argument.ProviderAnnotation;
import me.vaperion.blade.command.bindings.impl.DefaultBindings;
import me.vaperion.blade.command.command.BladeCommand;
import me.vaperion.blade.command.container.ContainerCreator;
import me.vaperion.blade.command.container.ICommandContainer;
import me.vaperion.blade.command.help.HelpGenerator;
import me.vaperion.blade.command.help.impl.DefaultHelpGenerator;
import me.vaperion.blade.command.tabcompleter.TabCompleter;
import me.vaperion.blade.command.tabcompleter.impl.DefaultTabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BladeCommandService {

    final List<BladeProviderContainer<?>> providers = new LinkedList<>();
    final List<BladeCommand> commands = new LinkedList<>();
    final Map<String, List<BladeCommand>> aliasCommands = new LinkedHashMap<>();
    final Map<String, ICommandContainer> containerMap = new LinkedHashMap<>();

    @Setter @Getter private boolean overrideCommands = false;
    @Setter @Getter private ContainerCreator<?> containerCreator = ContainerCreator.NONE;
    @Setter @Getter private TabCompleter tabCompleter = new DefaultTabCompleter();
    @Setter @Getter private HelpGenerator helpGenerator = new DefaultHelpGenerator();
    @Setter @Getter private Consumer<Runnable> asyncExecutor = Runnable::run;
    @Setter @Getter private long executionTimeWarningThreshold = 5;
    @Setter @Getter private String defaultPermissionMessage = "You don't have permission to perform this command.";

    @Getter private final BladeCommandRegistrar commandRegistrar = new BladeCommandRegistrar(this);
    @Getter private final BladeCommandResolver commandResolver = new BladeCommandResolver(this);
    @Getter private final BladeCommandParser commandParser = new BladeCommandParser(this);
    @Getter private final BladeCommandCompleter commandCompleter = new BladeCommandCompleter(this);

    public BladeCommandService() {
        new DefaultBindings().bind(this);
    }

    @NotNull
    public List<BladeCommand> getAllBladeCommands() {
        return Collections.unmodifiableList(this.aliasCommands.values().stream().flatMap(List::stream).collect(Collectors.toList()));
    }

    @NotNull
    public Map<String, ICommandContainer> getRegisteredCommands() {
        return Collections.unmodifiableMap(this.containerMap);
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
