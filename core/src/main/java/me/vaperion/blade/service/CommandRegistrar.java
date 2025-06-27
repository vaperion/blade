package me.vaperion.blade.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.command.Command;
import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static me.vaperion.blade.util.Preconditions.mustGetAnnotation;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class CommandRegistrar {

    private static final List<BladeCommand> EMPTY_COMMAND_LIST = new ArrayList<>();

    private final Blade blade;

    @ApiStatus.Internal
    public void registerClass(@Nullable Object instance,
                              @NotNull Class<?> clazz) {
        try {
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(Command.class)) continue;
                if ((instance == null) != Modifier.isStatic(method.getModifiers())) continue;

                registerMethod(instance, method);
            }

            blade.getPlatform().postCommandMapUpdate();
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while registering %s commands in class %s",
                instance == null ? "static" : "instance", clazz.getCanonicalName());
        }
    }

    @ApiStatus.Internal
    public void unregisterClass(@Nullable Object instance,
                                @NotNull Class<?> clazz) {
        try {
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(Command.class)) continue;
                if ((instance == null) != Modifier.isStatic(method.getModifiers())) continue;

                unregisterMethod(instance, method);
            }

            blade.getPlatform().postCommandMapUpdate();
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while unregistering %s commands in class %s",
                instance == null ? "static" : "instance", clazz.getCanonicalName());
        }
    }

    @ApiStatus.Internal
    public void registerMethod(@Nullable Object instance,
                               @NotNull Method method) throws Exception {
        BladeCommand cmd = new BladeCommand(blade, instance, method);
        blade.getCommands().add(cmd);

        for (String alias : cmd.getAliases()) {
            String realAlias = alias.split(" ")[0];

            blade.getAliasToCommands()
                .computeIfAbsent(realAlias, $ -> new LinkedList<>())
                .add(cmd);

            if (blade.getContainers().containsKey(realAlias)) continue;

            blade.getContainers()
                .put(realAlias, blade.getPlatform().getContainerCreator()
                    .create(blade, cmd, realAlias));
        }
    }

    @ApiStatus.Internal
    public void unregisterMethod(@Nullable Object instance,
                                 @NotNull Method method) {
        Command command =
            mustGetAnnotation(method, Command.class);

        String[] aliases = command.value();
        aliases = Arrays.stream(aliases).map(String::toLowerCase).toArray(String[]::new);

        BladeCommand cmd = blade.getCommands().stream().filter(c -> c.getInstance() == instance && c.getMethod() == method).findFirst().orElse(null);
        if (cmd == null) return;
        blade.getCommands().remove(cmd);

        for (String alias : aliases) {
            String realAlias = alias.split(" ")[0];

            List<BladeCommand> commandList = blade.getAliasToCommands()
                .getOrDefault(realAlias, EMPTY_COMMAND_LIST);

            commandList.remove(cmd);
            if (commandList.isEmpty())
                blade.getAliasToCommands().remove(realAlias);
        }
    }

    @ApiStatus.Internal
    public void unregisterAlias(@NotNull String alias) {
        List<Runnable> calls = new ArrayList<>();

        for (BladeCommand command : blade.getCommands()) {
            String[] aliases = command.getAliases();

            if (Arrays.stream(aliases)
                .noneMatch(a -> a.equalsIgnoreCase(alias))) continue;

            calls.add(() -> unregisterMethod(command.getInstance(), command.getMethod()));
        }

        calls.forEach(Runnable::run);
    }
}