package me.vaperion.blade.platform;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.vaperion.blade.annotation.command.Async;
import me.vaperion.blade.annotation.command.Permission;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.log.BladeLogger;
import me.vaperion.blade.util.Preconditions;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Setter
@Getter
public final class BladeConfiguration<Text> {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(
        r -> {
            Thread thread = new Thread(r);
            thread.setName("blade-async-executor-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        }
    );

    /**
     * Whether Blade should override vanilla commands when registering commands with the same name.
     * <p>
     * For example, if you register a command named {@code whitelist}, the vanilla {@code /whitelist} command
     * will be overridden if this option is enabled.
     */
    private boolean overrideCommands;

    /**
     * The qualifier used to register commands.
     * <p>
     * Most platforms (Bukkit, Velocity, etc.) prefix commands with the plugin name. As an example, you can access
     * the plugins command in Bukkit using {@code /bukkit:plugins} as well as {@code /plugins}.
     * <p>
     * By default, this is set to your plugin's name.
     */
    private String commandQualifier;

    /**
     * The default permission message sent to users when they don't have permission to execute a command.
     * <p>
     * This can be overridden on a per-command basis using the {@link Permission} annotation.
     */
    private String defaultPermissionMessage = "You don't have permission to perform this command.";

    /**
     * The execution time threshold (in milliseconds) after which a warning is logged when a command takes too long to execute on the main thread.
     */
    private long executionTimeWarningThreshold = 25L;

    /**
     * Whether Blade should strictly enforce the argument count when executing commands.
     * <p>
     * When enabled (default), if a user provides more arguments than a command expects, Blade will reject the command execution
     * and inform the user about the incorrect argument count.
     */
    private boolean strictArgumentCount = true;

    /**
     * Whether Blade should strictly match flags when parsing command arguments.
     * <p>
     * When disabled (default), any unknown flags that are encountered will be parsed as standard arguments.
     * This also applies to BSD-style flags, e.g. {@code -ab} will be parsed as an argument if either {@code -a} or {@code -b} are unknown.
     * <p>
     * When enabled, unknown flags will simply be ignored during parsing.
     */
    private boolean lenientFlagMatching = false;

    /**
     * Whether Blade should strictly enforce that each flag is only provided once.
     * <p>
     * When enabled (default), if a user provides the same flag multiple times, Blade will reject the command execution
     * and inform the user about the duplicate flags.
     */
    private boolean strictFlagCount = true;

    /**
     * The executor used for running asynchronous commands (commands annotated with {@link Async}).
     */
    private Consumer<Runnable> asyncExecutor = EXECUTOR_SERVICE::execute;

    /**
     * The tab completer used for providing command completions.
     * <p>
     * By default, a platform-specific implementation is used.
     */
    private TabCompleter tabCompleter = new TabCompleter.Default();

    /**
     * The help generator used for generating help messages for commands.
     */
    private HelpGenerator<Text> helpGenerator = new HelpGenerator.Default<>();

    /**
     * The logger Blade uses to log messages, warnings and errors.
     */
    private BladeLogger logger = BladeLogger.DEFAULT;

    /**
     * The comparator used to sort commands in help messages.
     * <p>
     * By default, commands are sorted alphabetically by their main label.
     */
    private Comparator<BladeCommand> helpSorter = Comparator.comparing(BladeCommand::mainLabel);

    public void validate() {
        Preconditions.checkNotNull(commandQualifier, "Command qualifier cannot be null.");
        Preconditions.checkNotNull(helpGenerator, "Help generator cannot be null.");
        Preconditions.checkNotNull(tabCompleter, "Tab completer cannot be null.");
    }

}
