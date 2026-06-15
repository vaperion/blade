package me.vaperion.blade;

import me.vaperion.blade.context.Context;
import me.vaperion.blade.exception.BladeParseError;
import me.vaperion.blade.impl.node.ResolvedCommand;
import me.vaperion.blade.impl.suggestions.SuggestionType;
import me.vaperion.blade.test.BladeTestPlatform;
import me.vaperion.blade.test.commands.OverloadedCommands;
import me.vaperion.blade.test.platform.TestSender;
import me.vaperion.blade.tree.CommandTreeNode;
import me.vaperion.blade.util.ErrorMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class OverloadTest {

    @BeforeEach
    public void reset() {
        OverloadedCommands.reset();
    }

    @Test
    public void overloadsShareSingleTreeNode() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        CommandTreeNode tp = blade.commandTree().root("tp");

        Assertions.assertNotNull(tp);
        Assertions.assertTrue(tp.isLeaf());
        Assertions.assertEquals(3, tp.commands().size());

        Assertions.assertSame(tp.commands().get(0), tp.command());
    }

    @Test
    public void resolvedCommandExposesAllOverloads() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        ResolvedCommand node = blade.nodeResolver().resolve("/tp a b c");

        Assertions.assertNotNull(node);
        Assertions.assertFalse(node.isStub());
        Assertions.assertEquals(3, node.overloads().size());
    }

    @Test
    public void dispatchesByArgumentCount() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        Assertions.assertNull(execute(blade, "tp foo"));
        Assertions.assertNull(execute(blade, "tp foo bar"));
        Assertions.assertNull(execute(blade, "tp 1 2 3"));

        Assertions.assertEquals(
            Arrays.asList(
                "destination=foo",
                "who=foo,destination=bar",
                "x=1,y=2,z=3"
            ),
            OverloadedCommands.INVOCATIONS
        );
    }

    @Test
    public void dispatchesByArgumentType() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        Assertions.assertNull(execute(blade, "give 5"));
        Assertions.assertNull(execute(blade, "give sword"));

        Assertions.assertEquals(
            Arrays.asList("amount=5", "item=sword"),
            OverloadedCommands.INVOCATIONS
        );
    }

    @Test
    public void reportsParseErrorWhenNoOverloadMatches() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        Assertions.assertThrows(BladeParseError.class,
            () -> execute(blade, "mode BLUE"));

        Assertions.assertTrue(OverloadedCommands.INVOCATIONS.isEmpty());
    }

    @Test
    public void showsIncompleteOverloadUsageWhenAllTypedTokensBind() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        ErrorMessage error = execute(blade, "fly 1 2");

        Assertions.assertNotNull(error);
        Assertions.assertEquals(ErrorMessage.Type.SHOW_COMMAND_USAGE, error.type());
        Assertions.assertNotNull(error.command());
        Assertions.assertEquals(3, error.command().arguments().size());
        Assertions.assertTrue(OverloadedCommands.INVOCATIONS.isEmpty());
    }

    @Test
    public void prefersDeepestParseErrorWhenNoOverloadMatches() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        BladeParseError error = Assertions.assertThrows(BladeParseError.class,
            () -> execute(blade, "paint RED BLUE"));

        Assertions.assertTrue(error.getMessage().contains("RED, GREEN"),
            "unexpected error message: " + error.getMessage());

        Assertions.assertTrue(OverloadedCommands.INVOCATIONS.isEmpty());
    }

    @Test
    public void prefersTooManyArgumentsErrorWhenAnOverloadFullyBinds() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        BladeParseError error = Assertions.assertThrows(BladeParseError.class,
            () -> execute(blade, "tp a b c"));

        Assertions.assertEquals(
            "Too many arguments. Please remove the last 1 argument.",
            error.getMessage()
        );

        Assertions.assertTrue(OverloadedCommands.INVOCATIONS.isEmpty());
    }

    @Test
    public void reportsUsageWhenNoOverloadAcceptsArgumentCount() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        ErrorMessage error = execute(blade, "tp");

        Assertions.assertNotNull(error);
        Assertions.assertEquals(ErrorMessage.Type.SHOW_COMMAND_USAGE, error.type());
        Assertions.assertTrue(OverloadedCommands.INVOCATIONS.isEmpty());
    }

    @Test
    public void mergesSuggestionsAcrossOverloads() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        ResolvedCommand node = blade.nodeResolver().resolve("/mode");
        Assertions.assertNotNull(node);

        Context context = new Context(blade, new TestSender(), "mode", new String[]{ "" });

        List<String> suggestions = blade.suggestionProvider().suggestNode(
            context,
            node,
            "/mode ",
            SuggestionType.ARGUMENTS
        );

        Assertions.assertEquals(
            new HashSet<>(Arrays.asList("RED", "GREEN", "CIRCLE", "SQUARE")),
            new HashSet<>(suggestions)
        );
    }

    @Test
    public void suggestionsOnlyComeFromOverloadsMatchingTypedArguments() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        ResolvedCommand node = blade.nodeResolver().resolve("/paint");
        Assertions.assertNotNull(node);

        Context context = new Context(blade, new TestSender(), "paint", new String[]{ "" });

        Assertions.assertEquals(
            new HashSet<>(Arrays.asList("RED", "GREEN", "CIRCLE", "SQUARE")),
            new HashSet<>(blade.suggestionProvider().suggestNode(
                context, node, "/paint ", SuggestionType.ARGUMENTS))
        );

        Assertions.assertEquals(
            new HashSet<>(Arrays.asList("RED", "GREEN")),
            new HashSet<>(blade.suggestionProvider().suggestNode(
                context, node, "/paint RED ", SuggestionType.ARGUMENTS))
        );

        Assertions.assertEquals(
            new HashSet<>(Arrays.asList("CIRCLE", "SQUARE")),
            new HashSet<>(blade.suggestionProvider().suggestNode(
                context, node, "/paint SQUARE ", SuggestionType.ARGUMENTS))
        );
    }

    @Test
    public void unregisteringOneOverloadKeepsTheOthers() throws Exception {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        Method method = OverloadedCommands.class.getDeclaredMethod("tpDestination", String.class);
        blade.registrar().unregisterMethod(null, method);

        CommandTreeNode tp = blade.commandTree().root("tp");

        Assertions.assertNotNull(tp);
        Assertions.assertEquals(2, tp.commands().size());

        ErrorMessage error = execute(blade, "tp 7");

        Assertions.assertNotNull(error);
        Assertions.assertEquals(ErrorMessage.Type.SHOW_COMMAND_USAGE, error.type());
        Assertions.assertTrue(OverloadedCommands.INVOCATIONS.isEmpty());
    }

    @Test
    public void unregisteringAllOverloadsRemovesTheRoot() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(OverloadedCommands.class);

        blade.registrar().unregisterLabel("tp");

        Assertions.assertNull(blade.commandTree().root("tp"));
        Assertions.assertNotNull(blade.commandTree().root("give"));
    }

    @Nullable
    private ErrorMessage execute(@NotNull Blade blade, @NotNull String commandLine) {
        ResolvedCommand node = blade.nodeResolver().resolve(commandLine);

        Assertions.assertNotNull(node);
        Assertions.assertFalse(node.isStub());

        String label = Objects.requireNonNull(node.matchedLabel());
        String[] args = commandLine.substring(label.length()).trim().split(" ");

        Context context = new Context(blade, new TestSender(), label, args);

        return blade.executor().execute(context, node, "/" + commandLine);
    }

}
