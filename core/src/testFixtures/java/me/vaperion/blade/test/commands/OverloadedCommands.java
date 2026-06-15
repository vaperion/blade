package me.vaperion.blade.test.commands;

import me.vaperion.blade.annotation.command.Command;
import me.vaperion.blade.annotation.parameter.Name;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class OverloadedCommands {

    public static final List<String> INVOCATIONS = new ArrayList<>();

    private OverloadedCommands() {
    }

    public static void reset() {
        INVOCATIONS.clear();
    }

    @Command("tp")
    public static void tpDestination(@Name("destination") @NotNull String destination) {
        INVOCATIONS.add("destination=" + destination);
    }

    @Command("tp")
    public static void tpWhoDestination(@Name("who") @NotNull String who,
                                        @Name("destination") @NotNull String destination) {
        INVOCATIONS.add("who=" + who + ",destination=" + destination);
    }

    @Command("tp")
    public static void tpCoordinates(@Name("x") int x,
                                     @Name("y") int y,
                                     @Name("z") int z) {
        INVOCATIONS.add("x=" + x + ",y=" + y + ",z=" + z);
    }

    @Command("give")
    public static void giveAmount(@Name("amount") int amount) {
        INVOCATIONS.add("amount=" + amount);
    }

    @Command("give")
    public static void giveItem(@Name("item") @NotNull String item) {
        INVOCATIONS.add("item=" + item);
    }

    @Command("mode")
    public static void modeColor(@Name("color") @NotNull Color color) {
        INVOCATIONS.add("color=" + color);
    }

    @Command("mode")
    public static void modeShape(@Name("shape") @NotNull Shape shape) {
        INVOCATIONS.add("shape=" + shape);
    }

    @Command("fly")
    public static void flyTo(@Name("x") int x,
                             @Name("y") int y,
                             @Name("z") int z) {
        INVOCATIONS.add("flyTo=" + x + "," + y + "," + z);
    }

    @Command("fly")
    public static void flySpeed(@Name("speed") int speed) {
        INVOCATIONS.add("flySpeed=" + speed);
    }

    @Command("paint")
    public static void paintColors(@Name("first") @NotNull Color first,
                                   @Name("second") @NotNull Color second) {
        INVOCATIONS.add("colors=" + first + "," + second);
    }

    @Command("paint")
    public static void paintShapes(@Name("first") @NotNull Shape first,
                                   @Name("second") @NotNull Shape second) {
        INVOCATIONS.add("shapes=" + first + "," + second);
    }

    public enum Color {RED, GREEN}

    public enum Shape {CIRCLE, SQUARE}

}
