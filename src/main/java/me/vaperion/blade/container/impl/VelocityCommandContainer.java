package me.vaperion.blade.container.impl;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.impl.VelocityUsageMessage;
import me.vaperion.blade.container.CommandContainer;
import me.vaperion.blade.container.ContainerCreator;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.context.impl.VelocitySender;
import me.vaperion.blade.exception.BladeExitMessage;
import me.vaperion.blade.exception.BladeUsageMessage;
import me.vaperion.blade.service.BladeCommandService;
import me.vaperion.blade.utils.Tuple;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class VelocityCommandContainer implements RawCommand, CommandContainer {

    public static final ContainerCreator<VelocityCommandContainer> CREATOR = VelocityCommandContainer::new;

    private final BladeCommandService commandService;
    private final BladeCommand parentCommand;

    private VelocityCommandContainer(@NotNull BladeCommandService service, @NotNull BladeCommand command, @NotNull String alias, @NotNull String fallbackPrefix) throws Exception {
        this.commandService = service;
        this.parentCommand = command;

        if (service.getVelocityProxyServer() == null) {
            throw new IllegalStateException("You must specify the proxy server in the Blade builder");
        }

        ProxyServer proxyServer = (ProxyServer) service.getVelocityProxyServer();
        CommandManager commandManager = proxyServer.getCommandManager();

        CommandMeta meta = commandManager.metaBuilder(alias)
              .aliases(command.getRealAliases())
              .build();
        commandManager.register(meta, this);
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

            case "ConsoleCommandSource":
                return "the console";

            default:
                return "everyone";
        }
    }

    private void sendUsageMessage(@NotNull BladeContext context, @Nullable BladeCommand command) {
        if (command == null) return;
        command.getUsageMessage().ensureGetOrLoad(() -> new VelocityUsageMessage(command)).sendTo(context);
    }

    private boolean hasPermission(@NotNull CommandSource sender, String[] args) throws BladeExitMessage {
        Tuple<BladeCommand, String> command = resolveCommand(joinAliasToArgs(this.parentCommand.getAliases()[0], args));
        BladeContext context = new BladeContext(commandService, new VelocitySender(sender), command == null ? "" : command.getRight(), args);
        return checkPermission(context, command == null ? null : command.getLeft()).getLeft();
    }

    private Tuple<Boolean, String> checkPermission(@NotNull BladeContext context, @Nullable BladeCommand command) throws BladeExitMessage {
        if (command == null)
            return new Tuple<>(false, "This command failed to execute as we couldn't find its registration.");

        return new Tuple<>(
              commandService.getPermissionTester().testPermission(context, command),
              command.isHidden() ? "" : command.getPermissionMessage());
    }

    private String[] joinAliasToArgs(String alias, String[] args) {
        String[] aliasParts = alias.split(" ");
        String[] argsWithAlias = new String[args.length + aliasParts.length];
        System.arraycopy(aliasParts, 0, argsWithAlias, 0, aliasParts.length);
        System.arraycopy(args, 0, argsWithAlias, aliasParts.length, args.length);
        return argsWithAlias;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return hasPermission(invocation.source(), new String[0]);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments().split(" ");
        String alias = invocation.alias();

        BladeCommand command = null;
        String resolvedAlias;

        String[] joined = joinAliasToArgs(alias, args);
        BladeContext context = new BladeContext(commandService, new VelocitySender(sender), alias, args);

        try {
            Tuple<BladeCommand, String> resolved = resolveCommand(joined);
            if (resolved == null) {
                List<BladeCommand> availableCommands = commandService.getAllBladeCommands()
                      .stream().filter(c -> Arrays.stream(c.getAliases()).anyMatch(a -> a.toLowerCase().startsWith(alias.toLowerCase(Locale.ROOT) + " ") || a.equalsIgnoreCase(alias)))
                      .filter(c -> this.checkPermission(context, c).getLeft())
                      .collect(Collectors.toList());

                for (String line : commandService.getHelpGenerator().generate(context, availableCommands)) {
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
                }

                return;
            }

            Tuple<Boolean, String> permissionResult = checkPermission(context, resolved.getLeft());
            if (!permissionResult.getLeft()) throw new BladeExitMessage(permissionResult.getRight());

            command = resolved.getLeft();
            resolvedAlias = resolved.getRight();
            int offset = Math.min(args.length, resolvedAlias.split(" ").length - 1);

            if (command.isSenderParameter() && !command.getSenderType().isInstance(sender))
                throw new BladeExitMessage("This command can only be executed by " + getSenderType(command.getSenderType()) + ".");

            final BladeCommand finalCommand = command;
            final String finalResolvedAlias = resolvedAlias;

            if (finalCommand.getMethod() == null) {
                throw new BladeExitMessage("The command " + finalResolvedAlias + " is a root command and cannot be executed.");
            }

            Runnable runnable = () -> {
                try {
                    List<Object> parsed;
                    if (finalCommand.isContextBased()) {
                        parsed = Collections.singletonList(context);
                    } else {
                        parsed = commandService.getCommandParser().parseArguments(finalCommand, context, Arrays.copyOfRange(args, offset, args.length));
                        if (finalCommand.isSenderParameter()) parsed.add(0, sender);
                    }

                    finalCommand.getMethod().invoke(finalCommand.getInstance(), parsed.toArray(new Object[0]));
                } catch (BladeUsageMessage ex) {
                    sendUsageMessage(context, finalCommand);
                } catch (BladeExitMessage ex) {
                    sender.sendMessage(Component.text(ex.getMessage()).color(NamedTextColor.RED));
                } catch (InvocationTargetException ex) {
                    if (ex.getTargetException() != null) {
                        if (ex.getTargetException() instanceof BladeUsageMessage) {
                            sendUsageMessage(context, finalCommand);
                            return;
                        } else if (ex.getTargetException() instanceof BladeExitMessage) {
                            sender.sendMessage(Component.text(ex.getTargetException().getMessage()).color(NamedTextColor.RED));
                            return;
                        }
                    }

                    ex.printStackTrace();
                    sender.sendMessage(Component.text("An exception was thrown while executing this command.").color(NamedTextColor.RED));
                } catch (Throwable t) {
                    t.printStackTrace();
                    sender.sendMessage(Component.text("An exception was thrown while executing this command.").color(NamedTextColor.RED));
                }
            };

            if (command.isAsync()) {
                commandService.getAsyncExecutor().accept(runnable);
            } else {
                long time = System.nanoTime();
                runnable.run();
                long elapsed = (System.nanoTime() - time) / 1000000;

                if (elapsed >= commandService.getExecutionTimeWarningThreshold()) {
                    System.out.printf(
                          "[Blade] Command '%s' (%s#%s) took %d milliseconds to execute!%n",
                          finalResolvedAlias,
                          finalCommand.getMethod().getDeclaringClass().getName(),
                          finalCommand.getMethod().getName(),
                          elapsed
                    );
                }
            }
        } catch (BladeUsageMessage ex) {
            sendUsageMessage(context, command);
        } catch (BladeExitMessage ex) {
            sender.sendMessage(Component.text(ex.getMessage()).color(NamedTextColor.RED));
        } catch (Throwable t) {
            t.printStackTrace();
            sender.sendMessage(Component.text("An exception was thrown while executing this command.").color(NamedTextColor.RED));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments().split(" ");
        String alias = invocation.alias();

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

            BladeContext context = new BladeContext(commandService, new VelocitySender(sender), foundAlias, actualArguments);

            List<String> suggestions = new ArrayList<>();
            commandService.getCommandCompleter().suggest(suggestions, context, command, actualArguments);
            return suggestions;
        } catch (BladeExitMessage ex) {
            sender.sendMessage(Component.text(ex.getMessage()).color(NamedTextColor.RED));
        } catch (Exception ex) {
            ex.printStackTrace();
            sender.sendMessage(Component.text("An exception was thrown while completing this command.").color(NamedTextColor.RED));
        }

        return Collections.emptyList();
    }
}
