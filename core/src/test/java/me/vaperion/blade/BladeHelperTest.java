package me.vaperion.blade;

import me.vaperion.blade.util.BladeHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BladeHelperTest {

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
