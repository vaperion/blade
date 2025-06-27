package me.vaperion.blade.bukkit.container;

import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.bukkit.command.BukkitUsageMessage;
import me.vaperion.blade.bukkit.context.BukkitSender;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.container.Container;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.exception.BladeUsageMessage;
import me.vaperion.blade.log.BladeLogger;
import me.vaperion.blade.util.BladeHelper;
import me.vaperion.blade.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.SimplePluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

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
    private final BladeCommand baseCommand;

    @SuppressWarnings("unchecked")
    private BukkitContainer(@NotNull Blade blade,
                            @NotNull BladeCommand command,
                            @NotNull String alias) throws Exception {
        super(alias, command.getDescription(), "/" + alias, new ArrayList<>());

        this.blade = blade;
        this.baseCommand = command;

        SimplePluginManager simplePluginManager = (SimplePluginManager) Bukkit.getServer().getPluginManager();
        SimpleCommandMap simpleCommandMap = (SimpleCommandMap) COMMAND_MAP.get(simplePluginManager);

        if (blade.getConfiguration().isOverrideCommands()) {
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

                if (doesBukkitCommandConflict(registeredCommand, alias, command)) {
                    registeredCommand.unregister(simpleCommandMap);

                    if (!lazyRemove)
                        iterator.remove();
                    else
                        keysToRemove.add(entry.getKey());
                }
            }

            keysToRemove.forEach(knownCommands::remove);
        }

        if (!simpleCommandMap.register(blade.getConfiguration().getFallbackPrefix(), this)) {
            System.err.println("Blade failed to register the command \"" + alias + "\". This could lead to issues.");
        }
    }

    private boolean doesBukkitCommandConflict(@NotNull Command bukkitCommand, @NotNull String alias, @NotNull BladeCommand command) {
        if (bukkitCommand instanceof BukkitContainer) return false; // don't override our own commands
        if (bukkitCommand.getName().equalsIgnoreCase(alias) || bukkitCommand.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(alias)))
            return true;
        for (String realAlias : command.getBaseCommands()) {
            if (bukkitCommand.getName().equalsIgnoreCase(realAlias) || bukkitCommand.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(realAlias)))
                return true;
        }
        return false;
    }

    @Nullable
    private Tuple<BladeCommand, String> resolveCommand(@NotNull String[] arguments) throws BladeExitMessage {
        return blade.getResolver().resolveCommand(arguments);
    }

    @Nullable
    private String getSenderTypeName(@NotNull Class<?> clazz) {
        switch (clazz.getSimpleName()) {
            case "Player":
                return "players";

            case "ConsoleCommandSender":
                return "the console";

            default:
                return null;
        }
    }

    private void sendUsageMessage(@NotNull Context context, @Nullable BladeCommand command) {
        if (command == null) return;
        command.getUsageMessage().ensureGetOrLoad(() -> new BukkitUsageMessage(command)).sendTo(context);
    }

    private boolean hasPermission(@NotNull CommandSender sender, String[] args) throws BladeExitMessage {
        Tuple<BladeCommand, String> command = resolveCommand(joinAliasToArgs(baseCommand.getAliases()[0], args));
        Context context = new Context(blade, new BukkitSender(sender), command == null ? "" : command.getRight(), args);
        return checkPermission(context, command == null ? null : command.getLeft()).getLeft();
    }

    private Tuple<Boolean, String> checkPermission(@NotNull Context context, @Nullable BladeCommand command) throws BladeExitMessage {
        if (command == null)
            return new Tuple<>(false, "This command failed to execute as we couldn't find its registration.");

        return new Tuple<>(
            blade.getPermissionTester().testPermission(context, command),
            command.isHidden() ? UNKNOWN_COMMAND_MESSAGE : command.getPermissionMessage());
    }

    private String[] joinAliasToArgs(String alias, String[] args) {
        String[] aliasParts = alias.split(" ");
        String[] argsWithAlias = new String[args.length + aliasParts.length];
        System.arraycopy(aliasParts, 0, argsWithAlias, 0, aliasParts.length);
        System.arraycopy(args, 0, argsWithAlias, aliasParts.length, args.length);
        return argsWithAlias;
    }

    @Override
    public boolean testPermissionSilent(@NotNull CommandSender sender) {
        return hasPermission(sender, new String[0]);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        BladeCommand command = null;
        String resolvedAlias;

        String[] joined = joinAliasToArgs(alias, args);
        Context context = new Context(blade, new BukkitSender(sender), alias, args);

        try {
            Tuple<BladeCommand, String> resolved = resolveCommand(joined);
            if (resolved == null) {
                List<BladeCommand> availableCommands = blade.getCommands()
                    .stream().filter(c -> Arrays.stream(c.getAliases()).anyMatch(a -> a.toLowerCase().startsWith(alias.toLowerCase(Locale.ROOT) + " ") || a.equalsIgnoreCase(alias)))
                    .collect(Collectors.toList());

                for (String line : blade.getConfiguration().getHelpGenerator().generate(context, availableCommands)) {
                    sender.sendMessage(line);
                }

                return true;
            }

            Tuple<Boolean, String> permissionResult = checkPermission(context, resolved.getLeft());
            if (!permissionResult.getLeft()) throw new BladeExitMessage(permissionResult.getRight());

            command = resolved.getLeft();
            resolvedAlias = resolved.getRight();
            int offset = Math.min(args.length, resolvedAlias.split(" ").length - 1);

            if (command.isHasSenderParameter()
                && !command.isWrappedSenderBased()
                && !command.isContextBased()
                && !command.getSenderType().isInstance(sender)) {
                String senderTypeName = getSenderTypeName(sender.getClass());

                if (senderTypeName != null) {
                    throw new BladeExitMessage("This command can only be executed by " + senderTypeName + ".");
                }

                // If it is null, the sender is probably a custom type.
                // We'll verify it later.
            }

            final BladeCommand finalCommand = command;
            final String finalResolvedAlias = resolvedAlias;

            if (finalCommand.getMethod() == null) {
                throw new BladeExitMessage("The command " + finalResolvedAlias + " is a root command and cannot be executed.");
            }

            Runnable runnable = () -> {
                try {
                    List<Object> methodArgs = BladeHelper.makeMethodArguments(
                        blade, finalCommand, context,
                        Arrays.copyOfRange(args, offset, args.length),
                        sender
                    );

                    finalCommand.getMethod().invoke(finalCommand.getInstance(),
                        methodArgs.toArray(new Object[0]));
                } catch (BladeUsageMessage ex) {
                    sendUsageMessage(context, finalCommand);
                } catch (BladeExitMessage ex) {
                    sender.sendMessage(ChatColor.RED + ex.getMessage());
                } catch (InvocationTargetException ex) {
                    if (ex.getTargetException() != null) {
                        if (ex.getTargetException() instanceof BladeUsageMessage) {
                            sendUsageMessage(context, finalCommand);
                            return;
                        } else if (ex.getTargetException() instanceof BladeExitMessage) {
                            sender.sendMessage(ChatColor.RED + ex.getTargetException().getMessage());
                            return;
                        }
                    }

                    blade.logger().error(ex, "An error occurred while %s was executing the command '%s' (%s#%s).",
                        sender.getName(), finalResolvedAlias, finalCommand.getMethod().getDeclaringClass().getName(), finalCommand.getMethod().getName());

                    sender.sendMessage(ChatColor.RED + "An error occurred while executing this command. Please contact the server administrator if this issue persists.");
                } catch (Throwable t) {
                    blade.logger().error(t, "An error occurred while %s was executing the command '%s' (%s#%s).",
                        sender.getName(), finalResolvedAlias, finalCommand.getMethod().getDeclaringClass().getName(), finalCommand.getMethod().getName());
                }
            };

            if (command.isAsync()) {
                blade.getConfiguration().getAsyncExecutor().accept(runnable);
            } else {
                long time = System.nanoTime();
                runnable.run();
                long elapsed = (System.nanoTime() - time) / 1000000;

                if (elapsed >= blade.getConfiguration().getExecutionTimeWarningThreshold()) {
                    blade.logger().warn(
                        "[Blade] Command '%s' (%s#%s) took %d milliseconds to execute!",
                        finalResolvedAlias,
                        finalCommand.getMethod().getDeclaringClass().getName(),
                        finalCommand.getMethod().getName(),
                        elapsed
                    );
                }
            }

            return true;
        } catch (BladeUsageMessage ex) {
            sendUsageMessage(context, command);
        } catch (BladeExitMessage ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while %s was executing the command '%s' (%s#%s).",
                sender.getName(), alias,
                command == null
                    ? "unknown"
                    : command.getMethod().getDeclaringClass().getName(),
                command == null
                    ? "unknown"
                    : command.getMethod().getName());
        }

        return false;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (!blade.getConfiguration().getTabCompleter().isDefault()) return Collections.emptyList();
        if (!hasPermission(sender, args)) return Collections.emptyList();

        BladeCommand command = null;

        try {
            Tuple<BladeCommand, String> resolved = resolveCommand(joinAliasToArgs(alias, args));
            if (resolved == null) {
                // maybe suggest subcommands?
                return Collections.emptyList();
            }

            command = resolved.getLeft();
            String foundAlias = resolved.getRight();

            List<String> argList = new ArrayList<>(Arrays.asList(args));
            if (foundAlias.split(" ").length > 1) argList.subList(0, foundAlias.split(" ").length - 1).clear();

            if (argList.isEmpty()) argList.add("");
            String[] actualArguments = argList.toArray(new String[0]);

            Context context = new Context(blade, new BukkitSender(sender), foundAlias, actualArguments);

            List<String> suggestions = new ArrayList<>();
            blade.getCompleter().suggest(suggestions, context, command, actualArguments);
            return suggestions;
        } catch (BladeExitMessage ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while %s was tab completing the command '%s' (%s#%s).",
                sender.getName(), alias,
                command == null
                    ? "unknown"
                    : command.getMethod().getDeclaringClass().getName(),
                command == null
                    ? "unknown"
                    : command.getMethod().getName());
        }

        return Collections.emptyList();
    }

}
