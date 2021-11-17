package me.vaperion.blade.service;

import lombok.Getter;
import lombok.Setter;
import me.vaperion.blade.argument.BladeProvider;
import me.vaperion.blade.argument.BladeProviderContainer;
import me.vaperion.blade.argument.ProviderAnnotation;
import me.vaperion.blade.bindings.impl.DefaultBindings;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.CommandContainer;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.help.HelpGenerator;
import me.vaperion.blade.help.impl.DefaultHelpGenerator;
import me.vaperion.blade.tabcompleter.TabCompleter;
import me.vaperion.blade.tabcompleter.impl.DefaultTabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BladeCommandService {

    final List<BladeProviderContainer<?>> providers = new LinkedList<>();
    final List<BladeCommand> commands = new LinkedList<>();
    final Map<String, List<BladeCommand>> aliasCommands = new LinkedHashMap<>();
    final Map<String, CommandContainer> containerMap = new LinkedHashMap<>();

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
    public Map<String, CommandContainer> getRegisteredCommands() {
        return Collections.unmodifiableMap(this.containerMap);
    }

    @SafeVarargs
    public final void releaseProvider(@NotNull Class<?> clazz, @NotNull Class<? extends ProviderAnnotation>... annotations) {
        releaseProvider(clazz, Arrays.asList(annotations));
    }

    public final void releaseProvider(@NotNull Class<?> clazz, @NotNull List<Class<? extends ProviderAnnotation>> annotations) {
        this.providers.removeIf(container -> container.getType() == clazz
              && annotations.containsAll(container.getRequiredAnnotations()) && container.getRequiredAnnotations().size() == annotations.size());
    }

    @SafeVarargs
    public final <T> void bindProvider(@NotNull Class<T> clazz, @NotNull BladeProvider<T> provider,
                                       @NotNull Class<? extends ProviderAnnotation>... annotations) {
        bindProvider(clazz, provider, Arrays.asList(annotations));
    }

    public final <T> void bindProvider(@NotNull Class<T> clazz, @NotNull BladeProvider<T> provider,
                                       @NotNull List<Class<? extends ProviderAnnotation>> annotations) {
        this.providers.add(new BladeProviderContainer<>(clazz, provider, annotations));
    }

    @Deprecated
    @SafeVarargs
    public final <T> void bindProviderUnsafely(@NotNull Class<T> clazz, @NotNull BladeProvider<?> provider,
                                               @NotNull Class<? extends ProviderAnnotation>... annotations) {
        bindProviderUnsafely(clazz, provider, Arrays.asList(annotations));
    }

    @Deprecated
    @SuppressWarnings({"unchecked", "DeprecatedIsStillUsed"})
    public final <T> void bindProviderUnsafely(@NotNull Class<T> clazz, @NotNull BladeProvider<?> provider,
                                               @NotNull List<Class<? extends ProviderAnnotation>> annotations) {
        this.providers.add(new BladeProviderContainer<>(clazz, (BladeProvider<T>) provider, annotations));
    }

}
