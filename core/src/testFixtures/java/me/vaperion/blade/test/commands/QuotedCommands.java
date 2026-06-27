package me.vaperion.blade.test.commands;

import me.vaperion.blade.annotation.command.Command;
import me.vaperion.blade.annotation.command.Quoted;
import me.vaperion.blade.annotation.parameter.Name;
import me.vaperion.blade.annotation.parameter.Opt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class QuotedCommands {

    public static final List<String> INVOCATIONS = new ArrayList<>();

    private QuotedCommands() {
    }

    public static void reset() {
        INVOCATIONS.clear();
    }

    @Command("delete")
    public static void delete(@Quoted @Name("id") @NotNull String id) {
        INVOCATIONS.add("id=" + id);
    }

    @Command("world load")
    public static void load(@Quoted @Name("id") @NotNull String id,
                            @Name("world name") @Opt @Nullable String worldName) {
        INVOCATIONS.add("id=" + id + ",worldName=" + worldName);
    }

    @Command("world marker add")
    public static void addMarker(@Quoted @Name("id") @NotNull String id,
                                 @Name("marker") @NotNull String marker) {
        INVOCATIONS.add("id=" + id + ",marker=" + marker);
    }

    @Command({ "hi", "hello world" })
    public static void greet(@Quoted @Name("name") @NotNull String name) {
        INVOCATIONS.add("name=" + name);
    }
}
