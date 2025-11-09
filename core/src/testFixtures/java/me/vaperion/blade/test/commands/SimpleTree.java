package me.vaperion.blade.test.commands;

import me.vaperion.blade.annotation.command.Command;
import me.vaperion.blade.context.Context;
import org.jetbrains.annotations.NotNull;

public final class SimpleTree {

    private SimpleTree() {
    }

    @Command("a a")
    public static void aa(@NotNull Context ctx) {
    }

    @Command("a b")
    public static void ab(@NotNull Context ctx) {
    }

    @Command("b a a")
    public static void baa(@NotNull Context ctx) {
    }

    @Command("b a b")
    public static void bab(@NotNull Context ctx) {
    }

    @Command("b b a")
    public static void bba(@NotNull Context ctx) {
    }

    @Command("b b b")
    public static void bbb(@NotNull Context ctx) {
    }

}
