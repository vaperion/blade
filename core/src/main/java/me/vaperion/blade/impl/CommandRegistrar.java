package me.vaperion.blade.impl;

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
            int n = 0;

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(Command.class)) continue;
                if ((instance == null) != Modifier.isStatic(method.getModifiers())) continue;

                registerMethod(instance, method);
                n++;
            }

            if (n > 0) {
                blade.platform().triggerBrigadierSync();
                blade.logger().info("Registered %d command%s in class %s",
                    n, n == 1 ? "" : "s", clazz.getCanonicalName());
            }
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while registering %s commands in class %s",
                instance == null ? "static" : "instance", clazz.getCanonicalName());
        }
    }

    @ApiStatus.Internal
    public void unregisterClass(@Nullable Object instance,
                                @NotNull Class<?> clazz) {
        try {
            int n = 0;

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(Command.class)) continue;
                if ((instance == null) != Modifier.isStatic(method.getModifiers())) continue;

                unregisterMethod(instance, method);
                n++;
            }

            if (n > 0) {
                blade.platform().triggerBrigadierSync();
                blade.logger().info("Unregistered %d command%s in class %s",
                    n, n == 1 ? "" : "s", clazz.getCanonicalName());
            }
        } catch (Throwable t) {
            blade.logger().error(t, "An error occurred while unregistering %s commands in class %s",
                instance == null ? "static" : "instance", clazz.getCanonicalName());
        }
    }

    @ApiStatus.Internal
    public void registerMethod(@Nullable Object instance,
                               @NotNull Method method) throws Exception {
        BladeCommand cmd = new BladeCommand(blade, instance, method);
        blade.commands().add(cmd);

        for (String label : cmd.labels()) {
            String realLabel = label.split(" ")[0];

            blade.labelToCommands()
                .computeIfAbsent(realLabel, $ -> new LinkedList<>())
                .add(cmd);

            if (blade.labelToContainer().containsKey(realLabel)) continue;

            blade.labelToContainer()
                .put(realLabel, blade.platform().containerCreator()
                    .create(blade, cmd, realLabel));
        }
    }

    @ApiStatus.Internal
    public void unregisterMethod(@Nullable Object instance,
                                 @NotNull Method method) {
        Command command =
            mustGetAnnotation(method, Command.class);

        String[] labels = command.value();
        labels = Arrays.stream(labels).map(String::toLowerCase).toArray(String[]::new);

        BladeCommand cmd = blade.commands().stream().filter(
                c -> c.instance() == instance && c.method() == method)
            .findFirst().orElse(null);
        if (cmd == null) return;

        blade.commands().remove(cmd);

        for (String label : labels) {
            String realLabel = label.split(" ")[0];

            List<BladeCommand> commandList = blade.labelToCommands()
                .getOrDefault(realLabel, EMPTY_COMMAND_LIST);

            commandList.remove(cmd);
            if (commandList.isEmpty())
                blade.labelToCommands().remove(realLabel);
        }
    }

    @ApiStatus.Internal
    public void unregisterLabel(@NotNull String label) {
        List<Runnable> calls = new ArrayList<>();

        for (BladeCommand command : blade.commands()) {
            String[] labels = command.labels();

            if (Arrays.stream(labels)
                .noneMatch(a -> a.equalsIgnoreCase(label))) continue;

            calls.add(() -> unregisterMethod(command.instance(), command.method()));
        }

        calls.forEach(Runnable::run);
    }
}