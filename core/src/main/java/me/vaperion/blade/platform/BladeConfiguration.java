package me.vaperion.blade.platform;

import lombok.Getter;
import lombok.Setter;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.log.BladeLogger;
import me.vaperion.blade.util.Preconditions;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Setter
@Getter
public final class BladeConfiguration {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private Object pluginInstance;

    private boolean overrideCommands;
    private String fallbackPrefix;
    private String defaultPermissionMessage = "You don't have permission to perform this command.";
    private long executionTimeWarningThreshold = 5L;

    private Consumer<Runnable> asyncExecutor = EXECUTOR_SERVICE::execute;

    private TabCompleter tabCompleter = new TabCompleter.Default();
    private HelpGenerator helpGenerator = new HelpGenerator.Default();
    private BladeLogger logger = BladeLogger.DEFAULT;

    private Comparator<BladeCommand> helpSorter = Comparator.comparing(BladeCommand::getUsageAlias);

    public void validate() {
        Preconditions.checkNotNull(pluginInstance, "Plugin instance cannot be null.");
        Preconditions.checkNotNull(fallbackPrefix, "Fallback prefix cannot be null.");
        Preconditions.checkNotNull(helpGenerator, "Help generator cannot be null.");
        Preconditions.checkNotNull(tabCompleter, "Tab completer cannot be null.");
    }

}
