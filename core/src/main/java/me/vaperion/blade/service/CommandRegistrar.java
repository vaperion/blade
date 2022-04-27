package me.vaperion.blade.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.command.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor
public class CommandRegistrar {

    private static final List<Command> EMPTY_COMMAND_LIST = new ArrayList<>();

    private final Blade blade;

    public void registerClass(@Nullable Object instance, @NotNull Class<?> clazz) {
        try {
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(me.vaperion.blade.annotation.Command.class)) continue;
                if ((instance == null) != Modifier.isStatic(method.getModifiers())) continue;

                registerMethod(instance, method);
            }
        } catch (Exception ex) {
            System.err.println("An exception was thrown while registering commands in class " + clazz.getCanonicalName() + " (instance: " + instance + ")");
            ex.printStackTrace();
        }
    }

    public void unregisterClass(@Nullable Object instance, @NotNull Class<?> clazz) {
        try {
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(me.vaperion.blade.annotation.Command.class)) continue;
                if ((instance == null) != Modifier.isStatic(method.getModifiers())) continue;

                unregisterMethod(instance, method);
            }
        } catch (Exception ex) {
            System.err.println("An exception was thrown while registering commands in class " + clazz.getCanonicalName() + " (instance: " + instance + ")");
            ex.printStackTrace();
        }
    }

    public void registerMethod(@Nullable Object instance, @NotNull Method method) throws Exception {
        me.vaperion.blade.annotation.Command command = method.getAnnotation(me.vaperion.blade.annotation.Command.class);

        String[] aliases = command.value();
        aliases = Arrays.stream(aliases).map(String::toLowerCase).toArray(String[]::new);

        Command cmd = new Command(blade, instance, method, aliases);
        blade.getCommands().add(cmd);

        for (String alias : aliases) {
            String realAlias = alias.split(" ")[0];

            blade.getAliasToCommands().computeIfAbsent(realAlias, $ -> new LinkedList<>()).add(cmd);

            if (blade.getContainers().containsKey(realAlias)) continue;
            blade.getContainers().put(realAlias, blade.getPlatform().getContainerCreator().create(blade, cmd, realAlias));
        }
    }

    public void unregisterMethod(@Nullable Object instance, @NotNull Method method) {
        me.vaperion.blade.annotation.Command command = method.getAnnotation(me.vaperion.blade.annotation.Command.class);

        String[] aliases = command.value();
        aliases = Arrays.stream(aliases).map(String::toLowerCase).toArray(String[]::new);

        Command cmd = blade.getCommands().stream().filter(c -> c.getInstance() == instance && c.getMethod() == method).findFirst().orElse(null);
        if (cmd == null) return;
        blade.getCommands().remove(cmd);

        for (String alias : aliases) {
            String realAlias = alias.split(" ")[0];

            List<Command> commandList = blade.getAliasToCommands().getOrDefault(realAlias, EMPTY_COMMAND_LIST);
            commandList.remove(cmd);
            if (commandList.isEmpty()) blade.getAliasToCommands().remove(realAlias);
        }
    }

    public void unregisterAlias(@NotNull String alias) {
        List<Runnable> calls = new ArrayList<>();

        for (Command command : blade.getCommands()) {
            String[] aliases = command.getAliases();
            if (Arrays.stream(aliases).noneMatch(a -> a.equalsIgnoreCase(alias))) continue;

            calls.add(() -> unregisterMethod(command.getInstance(), command.getMethod()));
        }

        calls.forEach(Runnable::run);
    }
}