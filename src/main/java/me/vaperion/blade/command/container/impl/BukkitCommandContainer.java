package me.vaperion.blade.command.container.impl;

import lombok.Getter;
import me.vaperion.blade.command.annotation.Flag;
import me.vaperion.blade.command.container.BladeCommand;
import me.vaperion.blade.command.container.BladeParameter;
import me.vaperion.blade.command.container.ContainerCreator;
import me.vaperion.blade.command.container.ICommandContainer;
import me.vaperion.blade.command.context.BladeContext;
import me.vaperion.blade.command.context.impl.BukkitSender;
import me.vaperion.blade.command.exception.BladeExitMessage;
import me.vaperion.blade.command.exception.BladeUsageMessage;
import me.vaperion.blade.command.service.BladeCommandService;
import me.vaperion.blade.utils.MessageBuilder;
import me.vaperion.blade.utils.Tuple;
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
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class BukkitCommandContainer extends Command implements ICommandContainer {

    private static final Field COMMAND_MAP, KNOWN_COMMANDS;
    public static final ContainerCreator<BukkitCommandContainer> CREATOR = BukkitCommandContainer::new;

    static {
        Field mapField = null, commandsField = null;

        try {
            mapField = SimplePluginManager.class.getDeclaredField("commandMap");
            mapField.setAccessible(true);
            commandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            commandsField.setAccessible(true);

            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);

            modifiers.setInt(mapField, modifiers.getInt(mapField) & ~Modifier.FINAL);
            modifiers.setInt(commandsField, modifiers.getInt(commandsField) & ~Modifier.FINAL);
        } catch (Exception ex) {
            System.err.println("Failed to grab commandMap from the plugin manager.");
            ex.printStackTrace();
        }

        COMMAND_MAP = mapField;
        KNOWN_COMMANDS = commandsField;
    }

    private final BladeCommandService commandService;
    private final BladeCommand parentCommand;

    @SuppressWarnings("unchecked")
    private BukkitCommandContainer(@NotNull BladeCommandService service, @NotNull BladeCommand command, @NotNull String alias) throws Exception {
        super(alias, command.getDescription(), "/" + alias, new ArrayList<>());

        this.commandService = service;
        this.parentCommand = command;

        SimplePluginManager simplePluginManager = (SimplePluginManager) Bukkit.getServer().getPluginManager();
        SimpleCommandMap simpleCommandMap = (SimpleCommandMap) COMMAND_MAP.get(simplePluginManager);

        if (service.isOverrideCommands()) {
            Map<String, Command> knownCommands = (Map<String, Command>) KNOWN_COMMANDS.get(simpleCommandMap);
            for (Command registeredCommand : new ArrayList<>(knownCommands.values())) {
                if (doesBukkitCommandConflict(registeredCommand, alias, command)) {
                    registeredCommand.unregister(simpleCommandMap);
                    knownCommands.remove(registeredCommand.getName().toLowerCase(Locale.ENGLISH));
                }
            }
            KNOWN_COMMANDS.set(simpleCommandMap, knownCommands);
        }

        simpleCommandMap.register(this.commandService.getFallbackPrefix(), this);
    }

    private boolean doesBukkitCommandConflict(@NotNull Command bukkitCommand, @NotNull String alias, @NotNull BladeCommand bladeCommand) {
        if (bukkitCommand instanceof BukkitCommandContainer) return false; // don't override our own commands
        if (bukkitCommand.getName().equalsIgnoreCase(alias) || bukkitCommand.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(alias)))
            return true;
        for (String realAlias : bladeCommand.getRealAliases()) {
            if (bukkitCommand.getName().equalsIgnoreCase(realAlias) || bukkitCommand.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(realAlias)))
                return true;
        }
        return false;
    }

    @Nullable
    private Tuple<BladeCommand, String> resolveCommand(@NotNull String[] arguments) throws BladeExitMessage {
        return commandService.getCommandResolver().resolveCommand(arguments);
    }

    @NotNull
    private String getSenderType(@NotNull Class<?> clazz) {
        switch (clazz.getSimpleName()) {
            case "Player":
                return "players";

            case "ConsoleCommandSender":
                return "the console";

            default:
                return "everyone";
        }
    }

    private void sendUsageMessage(@NotNull CommandSender sender, @NotNull String alias, @Nullable BladeCommand command) {
        if (command == null) return;
        boolean hasDesc = command.getDescription() != null && !command.getDescription().trim().isEmpty();

        MessageBuilder builder = new MessageBuilder(ChatColor.RED + "Usage: /").append(ChatColor.RED + alias);
        if (hasDesc) builder.hover(Collections.singletonList(ChatColor.GRAY + command.getDescription()));

        Optional.of(command.getFlagParameters())
              .ifPresent(flagParameters -> {
                  if (!flagParameters.isEmpty()) {
                      builder.append(" ").append(ChatColor.RED + "(").reset();
                      if (hasDesc)
                          builder.hover(Collections.singletonList(ChatColor.GRAY + command.getDescription().trim()));

                      int i = 0;
                      for (BladeParameter.FlagParameter flagParameter : flagParameters) {
                          builder.append(i++ == 0 ? "" : (ChatColor.GRAY + " | ")).reset();
                          if (hasDesc)
                              builder.hover(Collections.singletonList(ChatColor.GRAY + command.getDescription().trim()));

                          Flag flag = flagParameter.getFlag();

                          builder.append(ChatColor.AQUA + "-" + flag.value());
                          if (!flagParameter.isBooleanFlag())
                              builder.append(ChatColor.AQUA + " <" + flagParameter.getName() + ">");
                          if (!flag.description().trim().isEmpty())
                              builder.hover(Collections.singletonList(ChatColor.YELLOW + flag.description().trim()));
                      }

                      builder.append(ChatColor.RED + ")").reset();
                      if (hasDesc)
                          builder.hover(Collections.singletonList(ChatColor.GRAY + command.getDescription().trim()));
                  }
              });

        Optional.of(command.getCommandParameters())
              .ifPresent(commandParameters -> {
                  if (!commandParameters.isEmpty()) {
                      builder.append(" ");
                      if (hasDesc)
                          builder.hover(Collections.singletonList(ChatColor.GRAY + command.getDescription().trim()));

                      int i = 0;
                      for (BladeParameter.CommandParameter commandParameter : commandParameters) {
                          builder.append(i++ == 0 ? "" : " ");

                          builder.append(ChatColor.RED + (commandParameter.isOptional() ? "(" : "<"));
                          builder.append(ChatColor.RED + commandParameter.getName());
                          builder.append(ChatColor.RED + (commandParameter.isOptional() ? ")" : ">"));
                      }
                  }
              });

        if (command.getExtraUsageData() != null && !command.getExtraUsageData().trim().isEmpty()) {
            builder.append(" ");
            builder.append(ChatColor.RED + command.getExtraUsageData());
            if (hasDesc) builder.hover(Collections.singletonList(ChatColor.GRAY + command.getDescription().trim()));
        }

        builder.sendTo(sender);
    }

    private boolean hasPermission(@NotNull CommandSender sender, String[] args) throws BladeExitMessage {
        Tuple<BladeCommand, String> command = resolveCommand(joinAliasToArgs(this.parentCommand.getAliases()[0], args));
        return checkPermission(sender, command == null ? null : command.getLeft()).getLeft();
    }

    private Tuple<Boolean, String> checkPermission(@NotNull CommandSender sender, @Nullable BladeCommand command) throws BladeExitMessage {
        if (command == null)
            return new Tuple<>(false, "This command failed to execute as we couldn't find its registration.");
        if ("op".equals(command.getPermission())) return new Tuple<>(sender.isOp(), command.getPermissionMessage());
        if (command.getPermission() == null || command.getPermission().trim().isEmpty())
            return new Tuple<>(true, command.getPermissionMessage());
        return new Tuple<>(sender.hasPermission(command.getPermission()), command.getPermissionMessage());
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
        String resolvedAlias = alias;

        try {
            String[] joined = joinAliasToArgs(alias, args);

            BladeContext context = new BladeContext(new BukkitSender(sender), alias, args);

            Tuple<BladeCommand, String> resolved = resolveCommand(joined);
            if (resolved == null) {
                List<BladeCommand> availableCommands = commandService.getAllBladeCommands()
                      .stream().filter(c -> Arrays.stream(c.getAliases()).anyMatch(a -> a.toLowerCase().startsWith(alias.toLowerCase(Locale.ROOT) + " ") || a.equalsIgnoreCase(alias)))
                      .filter(c -> this.checkPermission(sender, c).getLeft())
                      .collect(Collectors.toList());

                for (String line : commandService.getHelpGenerator().generate(context, availableCommands)) {
                    sender.sendMessage(line);
                }

                return true;
            }

            Tuple<Boolean, String> permissionResult = checkPermission(sender, resolved.getLeft());
            if (!permissionResult.getLeft()) throw new BladeExitMessage(permissionResult.getRight());

            command = resolved.getLeft();
            resolvedAlias = resolved.getRight();
            int offset = Math.min(args.length, resolvedAlias.split(" ").length - 1);

            if (command.isSenderParameter() && !command.getSenderType().isInstance(sender))
                throw new BladeExitMessage("This command can only be executed by " + getSenderType(command.getSenderType()) + ".");

            final BladeCommand finalCommand = command;
            final String finalResolvedAlias = resolvedAlias;

            Runnable runnable = () -> {
                try {
                    List<Object> parsed;
                    if (finalCommand.isContextBased()) {
                        parsed = Collections.singletonList(context);
                    } else {
                        parsed = commandService.getCommandParser().parseArguments(finalCommand, context, Arrays.copyOfRange(args, offset, args.length));
                        if (finalCommand.isSenderParameter()) parsed.add(0, sender);
                    }

                    finalCommand.getMethod().setAccessible(true);
                    finalCommand.getMethod().invoke(finalCommand.getInstance(), parsed.toArray(new Object[0]));
                } catch (BladeUsageMessage ex) {
                    sendUsageMessage(sender, finalResolvedAlias, finalCommand);
                } catch (BladeExitMessage ex) {
                    sender.sendMessage(ChatColor.RED + ex.getMessage());
                } catch (InvocationTargetException ex) {
                    if (ex.getTargetException() != null) {
                        if (ex.getTargetException() instanceof BladeUsageMessage) {
                            sendUsageMessage(sender, finalResolvedAlias, finalCommand);
                            return;
                        } else if (ex.getTargetException() instanceof BladeExitMessage) {
                            sender.sendMessage(ChatColor.RED + ex.getTargetException().getMessage());
                            return;
                        }
                    }

                    ex.printStackTrace();
                    sender.sendMessage(ChatColor.RED + "An exception was thrown while executing this command.");
                } catch (Throwable t) {
                    t.printStackTrace();
                    sender.sendMessage(ChatColor.RED + "An exception was thrown while executing this command.");
                }
            };

            if (command.isAsync()) {
                commandService.getAsyncExecutor().accept(runnable);
            } else {
                long time = System.nanoTime();
                runnable.run();
                long elapsed = (System.nanoTime() - time) / 1000000;

                if (elapsed >= commandService.getExecutionTimeWarningThreshold()) {
                    Bukkit.getLogger().warning(String.format(
                          "[Blade] Command '%s' (%s#%s) took %d milliseconds to execute!",
                          finalResolvedAlias,
                          finalCommand.getMethod().getDeclaringClass().getName(),
                          finalCommand.getMethod().getName(),
                          elapsed
                    ));
                }
            }

            return true;
        } catch (BladeUsageMessage ex) {
            sendUsageMessage(sender, resolvedAlias, command);
        } catch (BladeExitMessage ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
            sender.sendMessage(ChatColor.RED + "An exception was thrown while executing this command.");
        }

        return false;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (!commandService.getTabCompleter().isDefault()) return Collections.emptyList();
        if (!hasPermission(sender, args)) return Collections.emptyList();

        try {
            Tuple<BladeCommand, String> resolved = resolveCommand(joinAliasToArgs(alias, args));
            if (resolved == null) {
                // maybe suggest subcommands?
                return Collections.emptyList();
            }

            BladeCommand command = resolved.getLeft();
            String foundAlias = resolved.getRight();

            List<String> argList = new ArrayList<>(Arrays.asList(args));
            if (foundAlias.split(" ").length > 1) argList.subList(0, foundAlias.split(" ").length - 1).clear();

            if (argList.isEmpty()) argList.add("");
            String[] actualArguments = argList.toArray(new String[0]);

            BladeContext context = new BladeContext(new BukkitSender(sender), foundAlias, actualArguments);
            return commandService.getCommandCompleter().suggest(context, command, actualArguments);
        } catch (BladeExitMessage ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            sender.sendMessage(ChatColor.RED + "An exception was thrown while completing this command.");
        }

        return Collections.emptyList();
    }
}
