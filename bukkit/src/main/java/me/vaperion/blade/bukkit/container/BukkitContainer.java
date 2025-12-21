package me.vaperion.blade.bukkit.container;

import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.bukkit.BladeBukkitPlatform;
import me.vaperion.blade.bukkit.command.BukkitInternalUsage;
import me.vaperion.blade.bukkit.context.BukkitSender;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.Container;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.exception.internal.BladeFatalError;
import me.vaperion.blade.exception.internal.BladeImplementationError;
import me.vaperion.blade.exception.internal.BladeInternalError;
import me.vaperion.blade.exception.internal.BladeInvocationError;
import me.vaperion.blade.impl.node.ResolvedCommand;
import me.vaperion.blade.impl.suggestions.SuggestionType;
import me.vaperion.blade.log.BladeLogger;
import me.vaperion.blade.tokenizer.TokenizerError;
import me.vaperion.blade.tokenizer.input.CommandInput;
import me.vaperion.blade.tokenizer.input.InputOption;
import me.vaperion.blade.util.ErrorMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.SimplePluginManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

import static me.vaperion.blade.util.BladeHelper.*;

@Getter
public final class BukkitContainer extends Command implements Container {

    public static final ContainerCreator<BukkitContainer> CREATOR = BukkitContainer::new;

    private static final Field COMMAND_MAP, KNOWN_COMMANDS;
    private static final String UNKNOWN_COMMAND_MESSAGE;

    static {
        Field mapField = null, commandsField = null;
        String unknownCommandMessage = ChatColor.WHITE + "Unknown command. Type \"/help\" for help.";

        try {
            Class<?> spigotConfigClass = Class.forName("org.spigotmc.SpigotConfig");
            Field unknownCommandField = spigotConfigClass.getDeclaredField("unknownCommandMessage");

            unknownCommandField.setAccessible(true);
            unknownCommandMessage = ChatColor.WHITE + (String) unknownCommandField.get(null);
        } catch (Throwable t) {
            BladeLogger.DEFAULT.error(t, "Failed to grab SpigotConfig#unknownCommandMessage. Using default message instead.");
        }

        try {
            mapField = SimplePluginManager.class.getDeclaredField("commandMap");
            commandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");

            mapField.setAccessible(true);
            commandsField.setAccessible(true);
        } catch (Throwable t) {
            BladeLogger.DEFAULT.error(t, "Failed to grab commandMap and knownCommands from the plugin manager!");
        }

        COMMAND_MAP = mapField;
        KNOWN_COMMANDS = commandsField;
        UNKNOWN_COMMAND_MESSAGE = unknownCommandMessage;
    }

    private final Blade blade;

    @SuppressWarnings("unchecked")
    private BukkitContainer(@NotNull Blade blade,
                            @NotNull String label) throws Exception {
        super(label, "", "/" + label, new ArrayList<>());

        this.blade = blade;

        SimplePluginManager simplePluginManager = (SimplePluginManager) Bukkit.getServer().getPluginManager();
        SimpleCommandMap simpleCommandMap = (SimpleCommandMap) COMMAND_MAP.get(simplePluginManager);

        if (blade.configuration().overrideCommands()) {
            Map<String, Command> knownCommands = (Map<String, Command>) KNOWN_COMMANDS.get(simpleCommandMap);
            Set<Map.Entry<String, Command>> entrySet = knownCommands.entrySet();
            Iterator<Map.Entry<String, Command>> iterator = entrySet.iterator();

            List<String> keysToRemove = new ArrayList<>();

            // Paper 1.21 and above provides a custom HashMap implementation that "transparently" forwards to Brigadier
            // Unfortunately this implementation doesn't support Iterator#remove, so we have to collect the keys
            boolean lazyRemove = entrySet.getClass().toString().contains("BukkitBrigForwardingMap");

            while (iterator.hasNext()) {
                Map.Entry<String, Command> entry = iterator.next();
                Command registeredCommand = entry.getValue();

                if (doesBukkitCommandConflict(registeredCommand, label)) {
                    registeredCommand.unregister(simpleCommandMap);

                    if (!lazyRemove)
                        iterator.remove();
                    else
                        keysToRemove.add(entry.getKey());
                }
            }

            keysToRemove.forEach(knownCommands::remove);
        }

        if (!simpleCommandMap.register(blade.configuration().commandQualifier(), this)) {
            System.err.println("Blade failed to register the command \"" + label + "\". This could lead to issues.");
        }
    }

    private boolean doesBukkitCommandConflict(@NotNull Command bukkitCommand,
                                              @NotNull String label) {
        if (bukkitCommand instanceof BukkitContainer)
            return false; // don't override our own commands

        return bukkitCommand.getName().equalsIgnoreCase(label) ||
            bukkitCommand.getAliases().stream()
                .anyMatch(a -> a.equalsIgnoreCase(label));
    }

    @Override
    public boolean testPermissionSilent(@NotNull CommandSender sender) {
        // Permission check is done in the execute method, as we don't know the exact command here.
        return true;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender,
                           @NotNull String label,
                           @NotNull String[] args) {
        String commandLine = removeCommandQualifier(
            mergeLabelWithArgs(label, args)
        );

        return execute(sender, commandLine);
    }

    public boolean execute(@NotNull CommandSender sender,
                           @NotNull String commandLine) {
        ResolvedCommand node = blade.nodeResolver().resolve(
            commandLine
        );

        if (node == null) {
            sender.sendMessage(UNKNOWN_COMMAND_MESSAGE);

            if (blade.configuration().verbose())
                blade.logger().info(
                    "%s tried to execute unknown command: `%s`. This is most likely a bug in Blade, not your plugin. Please report it.",
                    sender.getName(),
                    commandLine
                );
            return false;
        }

        String label = node.matchedLabelOr(
            commandLine.split(" ")[0]
        );

        String[] args = removePrefix(
            commandLine,
            label
        ).split(" ");

        Context context = new Context(
            blade,
            new BukkitSender(sender),
            label,
            args
        );

        if (node.isStub() || node.command() == null) {
            sendHelpMessage(sender,
                context,
                node.subcommands());
            return true;
        }

        BladeCommand command = node.command();

        if (!command.hasPermission(context)) {
            sender.sendMessage(ChatColor.RED + command.permissionMessage());
            return true;
        }

        try {
            Runnable runnable = () -> {
                try {
                    CommandInput input = command.tokenize(
                        context.sender(),
                        "/" + commandLine
                    );

                    if (!input.mergeTokensToFormWholeLabel(node.matchedLabel())) {
                        // Failed to merge label - can't execute command.
                        throw new BladeFatalError("Failed to parse command input for execution.");
                    }

                    ErrorMessage error = blade.executor().execute(context, input, node);

                    if (error != null) {
                        switch (error.type()) {
                            case LINES:
                                for (String line : error.lines()) {
                                    sender.sendMessage(ChatColor.RED + line);
                                }
                                break;

                            case SHOW_COMMAND_USAGE:
                                command.usageMessage().ensureGetOrLoad(
                                    () -> new BukkitInternalUsage(command, true)
                                ).sendTo(context);
                                break;
                        }
                    }
                } catch (BladeParseError | BladeFatalError e) {
                    sender.sendMessage(ChatColor.RED + e.getMessage());
                } catch (BladeInvocationError e) {
                    sender.sendMessage(ChatColor.RED + ERROR_MESSAGE);

                    blade.logger().error(e, "Blade failed to invoke the method for command `%s` executed by %s. This is most likely a bug in your plugin.",
                        label, sender.getName());
                } catch (BladeImplementationError e) {
                    sender.sendMessage(ChatColor.RED + ERROR_MESSAGE);
                    command.usageMessage().ensureGetOrLoad(
                        () -> new BukkitInternalUsage(command, true)
                    ).sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in your plugin.",
                        sender.getName(), label);
                } catch (BladeInternalError e) {
                    sender.sendMessage(ChatColor.RED + ERROR_MESSAGE);
                    command.usageMessage().ensureGetOrLoad(
                        () -> new BukkitInternalUsage(command, true)
                    ).sendTo(context);

                    blade.logger().error(e, "An internal error occurred while %s was executing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                        sender.getName(), label);
                } catch (TokenizerError error) {
                    sender.sendMessage(ChatColor.RED + error.formatForChat());
                    command.usageMessage().ensureGetOrLoad(
                        () -> new BukkitInternalUsage(command, true)
                    ).sendTo(context);

                    if (!error.type().isSilent()) {
                        blade.logger().error(
                            "Failed to parse %s's command input for command `%s`: %s",
                            sender.getName(),
                            label, TokenizerError.generateFancyMessage(error));
                    }
                } catch (Throwable t) {
                    blade.logger().error(t, "An unexpected error occurred while %s was executing the command `%s`.",
                        sender.getName(), label);
                }
            };

            if (command.async()) {
                blade.configuration().asyncExecutor().accept(runnable);
            } else {
                long time = System.nanoTime();
                runnable.run();
                long elapsed = (System.nanoTime() - time) / 1000000;

                if (elapsed >= blade.configuration().executionTimeWarningThreshold()) {
                    blade.logger().warn(
                        "Command `%s` (%s#%s) took %d milliseconds to execute!",
                        command.mainLabel(),
                        command.method().getDeclaringClass().getName(),
                        command.method().getName(),
                        elapsed
                    );
                }
            }

            return true;
        } catch (Throwable t) {
            blade.logger().error(t, "An unexpected error occurred while %s was executing the command `%s`.",
                sender.getName(), label);
        }

        return false;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender,
                                    @NotNull String label,
                                    @NotNull String[] args) throws IllegalArgumentException {
        String commandLine = mergeLabelWithArgs(label, args);

        return tabComplete(sender, commandLine, true);
    }

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender,
                                    @NotNull String commandLine) throws IllegalArgumentException {
        return tabComplete(sender, commandLine, false);
    }

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender,
                                    @NotNull String commandLine,
                                    boolean isBukkitCommand) throws IllegalArgumentException {
        if (!blade.configuration().tabCompleter().isDefault())
            return Collections.emptyList();

        ResolvedCommand node = blade.nodeResolver().resolve(
            commandLine
        );

        if (node == null) {
            // No main command and not a stub either - not a blade command at all?
            return Collections.emptyList();
        }

        try {
            EnumSet<SuggestionType> platformTypes = blade.platformAs(BladeBukkitPlatform.class).suggestionTypes();

            if (!node.isStub()) {
                // Found exact command, we can suggest arguments here.

                String[] args = removePrefix(
                    removeCommandQualifier(commandLine),
                    node.matchedLabelOr("")
                ).split(" ");

                if (!platformTypes.contains(SuggestionType.ARGUMENTS)) {
                    // Platform doesn't support argument suggestions.
                    return Collections.emptyList();
                }

                Context context = new Context(
                    blade,
                    new BukkitSender(sender),
                    node.matchedLabel(),
                    args
                );

                CommandInput input = node.command().tokenize(
                    context.sender(),
                    "/" + removeCommandQualifier(commandLine)
                );

                if (!input.mergeTokensToFormWholeLabel(node.matchedLabel())) {
                    // Failed to merge label - can't suggest arguments.
                    throw new BladeFatalError("Failed to parse command input for tab completion.");
                }

                return blade.suggestionProvider().suggest(
                    context,
                    input,
                    SuggestionType.ARGUMENTS
                );
            }

            // Only found command stub - suggest subcommands.

            if (!platformTypes.contains(SuggestionType.SUBCOMMANDS)) {
                // Platform doesn't support subcommand suggestions.
                return Collections.emptyList();
            }

            String[] args = removeCommandQualifier(commandLine).split(" ");

            Context context = new Context(
                blade,
                new BukkitSender(sender),
                "",
                args
            );

            CommandInput input = new CommandInput(
                blade,
                null,
                "/" + removeCommandQualifier(commandLine)
            );

            if (!isBukkitCommand)
                input.options().add(InputOption.DISALLOW_FLAGS);

            input.tokenize();

            return blade.suggestionProvider().suggest(
                context,
                input,
                SuggestionType.SUBCOMMANDS
            );
        } catch (BladeImplementationError e) {
            sender.sendMessage(ChatColor.RED + ERROR_MESSAGE);

            blade.logger().error(e, "An error occurred while %s was tab completing the command `%s`. This is a bug in your plugin.",
                sender.getName(), commandLine);
        } catch (BladeInternalError e) {
            sender.sendMessage(ChatColor.RED + ERROR_MESSAGE);

            blade.logger().error(e, "An error occurred while %s was tab completing the command `%s`. This is a bug in Blade, not your plugin. Please report it.",
                sender.getName(), commandLine);
        } catch (BladeFatalError ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        } catch (TokenizerError error) {
            // Don't send tokenizer errors to the user during tab completion - just log them.

            if (!error.type().isSilent()) {
                blade.logger().error(
                    "Failed to parse %s's command input for command `%s`: %s",
                    sender.getName(),
                    commandLine, TokenizerError.generateFancyMessage(error));
            }
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while %s was tab completing the command `%s`.",
                sender.getName(), commandLine);
        }

        return Collections.emptyList();
    }

    private void sendHelpMessage(@NotNull CommandSender sender,
                                 @NotNull Context context,
                                 @NotNull List<ResolvedCommand> nodes) {
        List<BladeCommand> allCommands = new ArrayList<>();

        nodes.forEach(node ->
            node.collectCommandsInto(allCommands));

        if (allCommands.isEmpty()) {
            sender.sendMessage(UNKNOWN_COMMAND_MESSAGE);
            return;
        }

        List<String> lines = blade.<String>configuration().helpGenerator().generate(context, allCommands);

        lines.forEach(sender::sendMessage);
    }

}
