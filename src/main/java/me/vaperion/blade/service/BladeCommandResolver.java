package me.vaperion.blade.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.argument.BladeProvider;
import me.vaperion.blade.argument.BladeProviderContainer;
import me.vaperion.blade.argument.ProviderAnnotation;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.utils.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BladeCommandResolver {

    private final BladeCommandService commandService;

    @Nullable
    public Tuple<BladeCommand, String> resolveCommand(@NotNull String[] input) {
        if (input.length == 0) return null;
        String[] commandParts = Arrays.copyOf(input, input.length);
        if (commandParts[0].contains(":")) commandParts[0] = commandParts[0].split(":", 2)[1];

        String baseCommand = commandParts[0].toLowerCase();
        List<BladeCommand> tree = commandService.aliasCommands.getOrDefault(baseCommand, new ArrayList<>());

        do {
            String checking = String.join(" ", commandParts);

            for (BladeCommand subCommand : tree) {
                for (String commandAlias : subCommand.getAliases()) {
                    if (commandAlias.equalsIgnoreCase(checking)) return new Tuple<>(subCommand, commandAlias);
                }
            }

            commandParts = Arrays.copyOfRange(commandParts, 0, commandParts.length - 1);
        } while (commandParts.length > 0);

        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> BladeProvider<T> resolveProvider(Class<T> clazz, List<Annotation> annotations) {
        List<Class<? extends ProviderAnnotation>> providerAnnotations = annotations.stream()
              .map(Annotation::annotationType)
              .filter(ProviderAnnotation.class::isAssignableFrom)
              .map(c -> (Class<? extends ProviderAnnotation>) c)
              .collect(Collectors.toList());

        return commandService.providers.stream()
              .filter(container -> container.getType() == clazz)
              .filter(container -> container.doAnnotationsMatch(providerAnnotations))
              .limit(1)
              .map(BladeProviderContainer::getProvider)
              .map(provider -> (BladeProvider<T>) provider)
              .findFirst().orElse(null);
    }

}
