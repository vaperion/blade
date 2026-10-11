package me.vaperion.blade.test.providers;

import me.vaperion.blade.argument.ArgumentProvider;
import me.vaperion.blade.argument.InputArgument;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.util.command.SuggestionsBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Suggests warp names that need quoting under brigadier's unquoted string rules, next to one that does not.
 */
public final class WarpNameProvider implements ArgumentProvider<String> {

    public static final List<String> NAMES = Arrays.asList("spawn", "hub/main", "end city", "say \"hi\"");

    @Override
    public @NotNull String provide(@NotNull Context ctx, @NotNull InputArgument arg) {
        return arg.requireValue();
    }

    @Override
    public void suggest(@NotNull Context ctx,
                        @NotNull InputArgument arg,
                        @NotNull SuggestionsBuilder suggestions) {
        for (String name : NAMES) {
            suggestions.suggest(name);
        }
    }
}
