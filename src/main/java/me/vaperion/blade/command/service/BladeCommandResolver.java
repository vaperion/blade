package me.vaperion.blade.command.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.command.argument.BladeProvider;
import me.vaperion.blade.command.container.BladeCommand;
import me.vaperion.blade.command.container.BladeProviderContainer;
import me.vaperion.blade.utils.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class BladeCommandResolver {

    private final BladeCommandService commandService;

    @Nullable
    public Tuple<BladeCommand, String> resolveCommand(@NotNull BladeCommand parentCommand, @NotNull String[] arguments) {
        String cmd = String.join(" ", arguments);

        do {
            AtomicReference<BladeCommand> commandRef = new AtomicReference<>();

            for (String alias : parentCommand.getRealAliases()) {
                final String finalCmd = cmd;

                Optional.ofNullable(commandService.aliasCommands.get(alias))
                        .ifPresent(list -> {
                            if (commandRef.get() != null) return;

                            list.forEach(command -> {
                                for (String commandAlias : command.getAliases()) {
                                    if (commandAlias.equalsIgnoreCase(finalCmd)) {
                                        commandRef.set(command);
                                        break;
                                    }
                                }
                            });
                        });
            }

            BladeCommand bladeCommand = commandRef.get();
            if (bladeCommand != null) return new Tuple<>(bladeCommand, cmd);

            String[] parts = cmd.split(" ");
            if (parts.length <= 1) cmd = "";
            else cmd = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1));
        } while (!cmd.isEmpty());

        return new Tuple<>(parentCommand, parentCommand.getRealAliases()[0]);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> BladeProvider<T> resolveProvider(Class<T> clazz, List<Annotation> annotations) {
        return commandService.providers.stream()
                .filter(container -> container.getType() == clazz)
                .filter(container -> container.getRequiredAnnotation() == null || annotations.stream().anyMatch(annotation -> annotation.getClass() == container.getRequiredAnnotation()))
                .limit(1)
                .map(BladeProviderContainer::getProvider)
                .map(provider -> (BladeProvider<T>) provider)
                .findFirst().orElse(null);
    }

}
