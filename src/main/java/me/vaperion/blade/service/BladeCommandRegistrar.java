package me.vaperion.blade.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.Command;
import me.vaperion.blade.annotation.Permission;
import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor
public class BladeCommandRegistrar {

    private final BladeCommandService commandService;

    public void registerClass(@Nullable Object instance, @NotNull Class<?> clazz, @NotNull String fallbackPrefix) {
        try {
            BladeCommand parent = null;

            if (clazz.isAnnotationPresent(Command.class)) {
                Command command = clazz.getAnnotation(Command.class);
                Permission permission = clazz.getAnnotation(Permission.class);
                parent = new BladeCommand(commandService, instance, null,
                      Arrays.stream(command.value()).map(String::toLowerCase).toArray(String[]::new), command, permission);
            }

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(Command.class)) continue;
                if ((instance == null) != Modifier.isStatic(method.getModifiers())) continue;

                registerMethod(instance, method, parent, fallbackPrefix);
            }
        } catch (Exception ex) {
            System.err.println("An exception was thrown while registering commands in class " + clazz.getCanonicalName() + " (instance: " + instance + ")");
            ex.printStackTrace();
        }
    }

    public void registerMethod(@Nullable Object instance, @NotNull Method method, @Nullable BladeCommand parentCommand, @NotNull String fallbackPrefix) throws Exception {
        Command command = method.getAnnotation(Command.class);
        Permission permission = method.getAnnotation(Permission.class);

        String[] aliases = parentCommand == null ? command.value() : mutateAliases(command.value(), parentCommand.getAliases());
        aliases = Arrays.stream(aliases).map(String::toLowerCase).toArray(String[]::new);

        BladeCommand bladeCommand = new BladeCommand(commandService, instance, method, aliases, command, permission);
        commandService.commands.add(bladeCommand);

        for (String alias : aliases) {
            String realAlias = alias.split(" ")[0];

            commandService.aliasCommands.computeIfAbsent(realAlias, $ -> new LinkedList<>()).add(bladeCommand);

            if (commandService.containerMap.containsKey(realAlias)) continue;
            commandService.containerMap.put(realAlias, commandService.getContainerCreator().create(commandService, bladeCommand, realAlias, fallbackPrefix));
        }
    }

    @NotNull
    private String[] mutateAliases(@NotNull String[] aliases, @NotNull String[] prefixes) {
        List<String> output = new LinkedList<>();

        for (String alias : aliases) {
            for (String prefix : prefixes) {
                output.add(String.format("%s %s", prefix, alias));
            }
        }

        return output.toArray(new String[0]);
    }

    @SuppressWarnings("UnusedReturnValue")
    public interface Registrar {
        @NotNull
        BladeCommandService commandService();

        @NotNull
        String fallbackPrefix();

        @NotNull
        Blade blade();

        default Blade exit() {
            return blade();
        }

        default void register(@Nullable Object instance, @NotNull Class<?> clazz) {
            commandService().getCommandRegistrar().registerClass(instance, clazz, fallbackPrefix());
        }

        @NotNull
        default Registrar register(@NotNull Class<?> containerClass) {
            register(null, containerClass);
            return this;
        }

        @NotNull
        default Registrar register(@NotNull Object containerInstance) {
            register(containerInstance, containerInstance.getClass());
            return this;
        }
    }
}
