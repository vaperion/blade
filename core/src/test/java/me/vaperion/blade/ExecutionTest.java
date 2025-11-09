package me.vaperion.blade;

import me.vaperion.blade.impl.node.ResolvedCommand;
import me.vaperion.blade.test.BladeTestPlatform;
import me.vaperion.blade.test.commands.SimpleTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExecutionTest {

    @Test
    public void simpleTreeExecution() {
        Blade blade = BladeTestPlatform.createInstance();

        blade.register(SimpleTree.class);

        ResolvedCommand ab = blade.nodeResolver().resolve("/a b hello world");

        Assertions.assertNotNull(ab);

        Assertions.assertFalse(ab.isStub());

        Assertions.assertEquals(0, ab.subcommands().size());

        Assertions.assertEquals(
            "a b",
            ab.command().mainLabel()
        );
    }

    @Test
    public void simpleTreeFirstStub() {
        Blade blade = BladeTestPlatform.createInstance();

        blade.register(SimpleTree.class);

        ResolvedCommand ab = blade.nodeResolver().resolve("/b");

        Assertions.assertNotNull(ab);

        Assertions.assertTrue(ab.isStub());

        Assertions.assertEquals(4, ab.subcommands().size());

        Assertions.assertArrayEquals(
            new String[]{ "b a a", "b a b", "b b a", "b b b" },
            ab.subcommands().stream()
                .map(c -> c.command().mainLabel())
                .toArray(String[]::new)
        );
    }

    @Test
    public void simpleTreeSecondStub() {
        Blade blade = BladeTestPlatform.createInstance();

        blade.register(SimpleTree.class);

        ResolvedCommand ab = blade.nodeResolver().resolve("/b a");

        Assertions.assertNotNull(ab);

        Assertions.assertTrue(ab.isStub());

        Assertions.assertEquals(2, ab.subcommands().size());

        Assertions.assertArrayEquals(
            new String[]{ "b a a", "b a b" },
            ab.subcommands().stream()
                .map(c -> c.command().mainLabel())
                .toArray(String[]::new)
        );
    }

}
