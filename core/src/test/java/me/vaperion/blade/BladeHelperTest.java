package me.vaperion.blade;

import me.vaperion.blade.util.BladeHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BladeHelperTest {

    @Test
    public void commandQualifierRemovalKeepsArgumentsOnce() {
        Assertions.assertEquals(
            "/lorem ipsum dolor",
            BladeHelper.removeCommandQualifier("/test:lorem ipsum dolor")
        );

        Assertions.assertEquals(
            "lorem ipsum dolor",
            BladeHelper.removeCommandQualifier("test:lorem ipsum dolor")
        );
    }

    @Test
    public void commandQualifierRemovalIgnoresArgumentColons() {
        Assertions.assertEquals(
            "/lorem ipsum foo:bar",
            BladeHelper.removeCommandQualifier("/lorem ipsum foo:bar")
        );
    }

    @Test
    public void suggestionArgumentsPreserveTrailingEmptyArgument() {
        Assertions.assertArrayEquals(
            new String[]{ "server", "" },
            BladeHelper.splitSuggestionArguments("server ")
        );
    }

    @Test
    public void suggestionArgumentsPreserveEmptyArgument() {
        Assertions.assertArrayEquals(
            new String[]{ "" },
            BladeHelper.splitSuggestionArguments("")
        );

        Assertions.assertArrayEquals(
            new String[]{ "" },
            BladeHelper.splitSuggestionArguments(" ")
        );
    }

    @Test
    public void suggestionArgumentsIgnoreLeadingSeparatorFromRemovedLabel() {
        Assertions.assertArrayEquals(
            new String[]{ "remove" },
            BladeHelper.splitSuggestionArguments(" remove")
        );
    }

}
