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

@Getter
public class BukkitCommandContainer extends Command implements ICommandContainer {

    private static final Field COMMAND_MAP, KNOWN_COMMANDS;
    public static final ContainerCreator<BukkitCommandContainer> CREATOR = BukkitCommandContainer::new;

    static {
        Field mapField = null, commansField = null;

        try {
            mapField = SimplePluginManager.class.getDeclaredField("commandMap");
            mapField.setAccessible(true);
            commansField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            commansField.setAccessible(true);

            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);

            modifiers.setInt(mapField, modifiers.getInt(mapField) & ~Modifier.FINAL);
            modifiers.setInt(commansField, modifiers.getInt(commansField) & ~Modifier.FINAL);
        } catch (Exception ex) {
            System.err.println("Failed to grab commandMap from the plugin manager.");
            ex.printStackTrace();
        }

        COMMAND_MAP = mapField;
        KNOWN_COMMANDS = commansField;
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
        if (bukkitCommand.getName().equalsIgnoreCase(alias) || bukkitCommand.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(alias))) return true;
        for (String realAlias : bladeCommand.getRealAliases()) {
            if (bukkitCommand.getName().equalsIgnoreCase(realAlias) || bukkitCommand.getAliases().stream().anyMatch(a -> a.equalsIgnoreCase(realAlias))) return true;
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

        MessageBuilder builder = new MessageBuilder(ChatColor.RED + "Usage: /").append(ChatColor.RED + alias);
        builder.hover(Collections.singletonList(ChatColor.GRAY + command.getDescription()));

        Optional.of(command.getFlagParameters())
                .ifPresent(flagParameters -> {
                    if (!flagParameters.isEmpty()) {
                        builder.append(" ").append(ChatColor.RED + "(");

                        int i = 0;
                        for (BladeParameter.FlagParameter flagParameter : flagParameters) {
                            builder.append(i++ == 0 ? "" : (ChatColor.GRAY + " | ")).reset();

                            Flag flag = flagParameter.getFlag();

                            builder.append(ChatColor.AQUA + "-" + flag.value());
                            if (!flagParameter.isBooleanFlag()) builder.append(ChatColor.AQUA + " <" + flagParameter.getName() + ">");
                            if (!flag.description().trim().isEmpty())
                                builder.hover(Collections.singletonList(ChatColor.YELLOW + flag.description().trim()));
                        }

                        builder.append(ChatColor.RED + ")");
                    }
                });

        Optional.of(command.getCommandParameters())
                .ifPresent(commandParameters -> {
                    if (!commandParameters.isEmpty()) {
                        builder.append(" ");
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

        builder.sendTo(sender);
    }

    private boolean hasPermission(@NotNull CommandSender sender, String[] args) throws BladeExitMessage {
        return checkPermission(sender, args).getLeft();
    }

    private Tuple<Boolean, String> checkPermission(@NotNull CommandSender sender, @NotNull String[] args) throws BladeExitMessage {
        BladeCommand command = resolveCommand(joinAliasToArgs(this.parentCommand.getAliases()[0], args)).getLeft();
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
            Tuple<Boolean, String> permissionResult = checkPermission(sender, args);
            if (!permissionResult.getLeft()) throw new BladeExitMessage(permissionResult.getRight());

            Tuple<BladeCommand, String> resolved = resolveCommand(joinAliasToArgs(alias, args));
            if (resolved == null) throw new BladeExitMessage("This command failed to execute as we couldn't find it's registration.");

            command = resolved.getLeft();
            resolvedAlias = resolved.getRight();
            int offset = Math.min(args.length, resolvedAlias.split(" ").length - 1);

            BladeContext context = new BladeContext(new BukkitSender(sender), alias, args);

            if (command.isSenderParameter() && !command.getSenderType().isInstance(sender))
                throw new BladeExitMessage("This command can only be executed by " + getSenderType(command.getSenderType()) + ".");

            List<Object> parsed;
            if (command.isContextBased()) {
                parsed = Collections.singletonList(context);
            } else {
                parsed = commandService.getCommandParser().parseArguments(command, context, Arrays.copyOfRange(args, offset, args.length));
                if (command.isSenderParameter()) parsed.add(0, sender);
            }

            try {
                command.getMethod().setAccessible(true);
                command.getMethod().invoke(command.getInstance(), parsed.toArray(new Object[0]));
            } catch (InvocationTargetException ex) {
                if (ex.getTargetException() != null && ex.getTargetException() instanceof BladeUsageMessage)
                    throw ex.getTargetException();
                throw ex;
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
