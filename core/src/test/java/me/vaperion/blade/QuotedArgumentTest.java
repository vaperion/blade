package me.vaperion.blade;

import me.vaperion.blade.context.Context;
import me.vaperion.blade.impl.node.ResolvedCommand;
import me.vaperion.blade.impl.suggestions.SuggestionType;
import me.vaperion.blade.test.BladeTestPlatform;
import me.vaperion.blade.test.commands.QuotedCommands;
import me.vaperion.blade.test.platform.TestSender;
import me.vaperion.blade.util.ErrorMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class QuotedArgumentTest {

    @BeforeEach
    public void setUp() {
        QuotedCommands.reset();
    }

    @Test
    public void quotedArgumentOnSingleWordLabel() {
        Blade blade = register();

        execute(blade, "delete \"hello world\"");

        Assertions.assertEquals(1, QuotedCommands.INVOCATIONS.size());
        Assertions.assertEquals("id=hello world", QuotedCommands.INVOCATIONS.get(0));
    }

    @Test
    public void quotedArgumentOnTwoWordLabel() {
        Blade blade = register();

        execute(blade, "world load \"hello world\" overworld");

        Assertions.assertEquals(1, QuotedCommands.INVOCATIONS.size());
        Assertions.assertEquals("id=hello world,worldName=overworld", QuotedCommands.INVOCATIONS.get(0));
    }

    @Test
    public void quotedArgumentOnThreeWordLabel() {
        Blade blade = register();

        execute(blade, "world marker add \"hello world\" spawn");

        Assertions.assertEquals(1, QuotedCommands.INVOCATIONS.size());
        Assertions.assertEquals("id=hello world,marker=spawn", QuotedCommands.INVOCATIONS.get(0));
    }

    @Test
    public void quotedArgumentOnShortAliasOfMultiLabelCommand() {
        Blade blade = register();

        execute(blade, "hi \"john doe\"");

        Assertions.assertEquals(1, QuotedCommands.INVOCATIONS.size());
        Assertions.assertEquals("name=john doe", QuotedCommands.INVOCATIONS.get(0));
    }

    @Test
    public void quotedArgumentOnLongAliasOfMultiLabelCommand() {
        Blade blade = register();

        execute(blade, "hello world \"john doe\"");

        Assertions.assertEquals(1, QuotedCommands.INVOCATIONS.size());
        Assertions.assertEquals("name=john doe", QuotedCommands.INVOCATIONS.get(0));
    }

    @Test
    public void quotedArgumentWithDifferentlyCasedLabel() {
        Blade blade = register();

        execute(blade, "WORLD LOAD \"hello world\" overworld");

        Assertions.assertEquals(1, QuotedCommands.INVOCATIONS.size());
        Assertions.assertEquals("id=hello world,worldName=overworld", QuotedCommands.INVOCATIONS.get(0));
    }

    @Test
    public void quotedArgumentSuggestionsAreQuotedWhenNeeded() {
        Blade blade = register();

        Assertions.assertEquals(
            new HashSet<>(Arrays.asList("spawn", "\"hub/main\"", "\"end city\"", "\"say \\\"hi\\\"\"")),
            new HashSet<>(suggest(blade, "warp", "/warp "))
        );
    }

    @Test
    public void quotedArgumentSuggestionsFilterOnUnquotedInput() {
        Blade blade = register();

        Assertions.assertEquals(
            Collections.singletonList("\"hub/main\""),
            suggest(blade, "warp", "/warp \"hu")
        );
    }

    @Test
    public void quotedSuggestionParsesBackToSuggestedValue() {
        Blade blade = register();

        execute(blade, "warp \"say \\\"hi\\\"\"");

        Assertions.assertEquals(1, QuotedCommands.INVOCATIONS.size());
        Assertions.assertEquals("name=say \"hi\"", QuotedCommands.INVOCATIONS.get(0));
    }

    @Test
    public void unquotedArgumentSuggestionsStayVerbatim() {
        Blade blade = register();

        Assertions.assertEquals(
            new HashSet<>(Arrays.asList("spawn", "hub/main", "end city", "say \"hi\"")),
            new HashSet<>(suggest(blade, "goto", "/goto "))
        );
    }

    @NotNull
    private List<String> suggest(@NotNull Blade blade, @NotNull String label, @NotNull String input) {
        ResolvedCommand node = blade.nodeResolver().resolve("/" + label);

        Assertions.assertNotNull(node);

        Context context = new Context(blade, new TestSender(), label, new String[]{ "" });

        return blade.suggestionProvider().suggestNode(context, node, input, SuggestionType.ARGUMENTS);
    }

    @NotNull
    private Blade register() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(QuotedCommands.class);
        return blade;
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
