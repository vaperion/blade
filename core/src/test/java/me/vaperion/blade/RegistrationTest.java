package me.vaperion.blade;

import me.vaperion.blade.test.BladeTestPlatform;
import me.vaperion.blade.test.commands.SimpleTree;
import me.vaperion.blade.tree.CommandTreeNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RegistrationTest {

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void simpleTree() {
        Blade blade = BladeTestPlatform.createInstance();

        blade.register(SimpleTree.class);

        Assertions.assertArrayEquals(
            new String[]{ "a", "b" },
            blade.commandTree().roots().keySet().toArray(new String[0])
        );

        CommandTreeNode a = blade.commandTree().root("a");

        Assertions.assertArrayEquals(
            new String[]{
                "a",
                "b"
            },
            a.children().keySet().toArray(new String[0])
        );

        CommandTreeNode b = blade.commandTree().root("b");

        Assertions.assertArrayEquals(
            new String[]{
                "a",
                "b"
            },
            b.children().keySet().toArray(new String[0])
        );

        Assertions.assertArrayEquals(
            new String[]{
                "a",
                "b"
            },
            b.child("a").children().keySet().toArray(new String[0])
        );

        Assertions.assertArrayEquals(
            new String[]{
                "a",
                "b"
            },
            b.child("b").children().keySet().toArray(new String[0])
        );
    }
}
