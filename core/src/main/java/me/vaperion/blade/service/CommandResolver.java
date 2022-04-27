package me.vaperion.blade.service;

import lombok.RequiredArgsConstructor;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.*;
import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.Provider;
import me.vaperion.blade.command.Command;
import me.vaperion.blade.util.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommandResolver {

    private static final List<Class<? extends Annotation>> INTERNAL_ANNOTATIONS = Arrays.asList(
          Text.class, me.vaperion.blade.annotation.Command.class, Completer.class, Data.class, Flag.class,
          Name.class, Optional.class, Permission.class, Range.class, Sender.class
    );

    private final Blade blade;

    @Nullable
    public Tuple<Command, String> resolveCommand(@NotNull String[] input) {
        if (input.length == 0) return null;
        String[] commandParts = Arrays.copyOf(input, input.length);
        if (commandParts[0].contains(":")) commandParts[0] = commandParts[0].split(":", 2)[1];

        String baseCommand = commandParts[0].toLowerCase();
        List<Command> tree = blade.getAliasToCommands().getOrDefault(baseCommand, new ArrayList<>());

        do {
            String checking = String.join(" ", commandParts);

            for (Command subCommand : tree) {
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
    public <T> ArgumentProvider<T> recursiveResolveProvider(Class<T> clazz, List<Annotation> annotations) {
        Class<?> parent = clazz;
        do {
            ArgumentProvider<T> provider = (ArgumentProvider<T>) resolveProvider(parent, annotations);
            if (provider != null) return provider;

            parent = parent.getSuperclass();
        } while (parent != Object.class);

        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> ArgumentProvider<T> resolveProvider(Class<T> clazz, List<Annotation> annotations) {
        List<Class<? extends Annotation>> inputAnnotations = annotations.stream()
              .map(Annotation::annotationType)
              .map(c -> (Class<? extends Annotation>) c)
              .collect(Collectors.toList());
        inputAnnotations.removeIf(INTERNAL_ANNOTATIONS::contains);

        return blade.getProviders().stream()
              .filter(container -> container.getType() == clazz)
              .filter(container -> container.doAnnotationsMatch(inputAnnotations))
              .limit(1)
              .map(Provider::getProvider)
              .map(provider -> (ArgumentProvider<T>) provider)
              .findFirst().orElse(null);
    }

}