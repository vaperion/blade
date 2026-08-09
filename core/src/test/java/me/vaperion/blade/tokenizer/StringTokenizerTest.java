package me.vaperion.blade.tokenizer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringTokenizerTest {

    @Test
    public void acceptsOneOfExpectedCharacters() {
        StringTokenizer tokenizer = new StringTokenizer(":");

        tokenizer.expectOneOf(';', ':');

        Assertions.assertEquals(':', tokenizer.take());
    }

    @Test
    public void logsUnexpectedCharactersByDefault() {
        StringTokenizer tokenizer = new StringTokenizer(":");

        TokenizerError error = Assertions.assertThrows(TokenizerError.class, () -> tokenizer.expect(';'));

        Assertions.assertTrue(error.type().shouldLog());
    }

    @Test
    public void reportsInvalidArgumentSeparatorAsUserInputError() {
        StringTokenizer tokenizer = new StringTokenizer(":");

        TokenizerError error = Assertions.assertThrows(TokenizerError.class, tokenizer::expectWhitespace);

        Assertions.assertEquals("expected whitespace but found ':'", error.getMessage());
        Assertions.assertFalse(error.type().shouldLog());
    }

    @Test
    public void reportsExpectedCharactersAsUserInputError() {
        StringTokenizer tokenizer = new StringTokenizer(":");

        TokenizerError error = Assertions.assertThrows(
            TokenizerError.class,
            () -> tokenizer.expectOneOf('"', '\'')
        );

        Assertions.assertEquals("expected one of ['\"', '\\''] but found ':'", error.getMessage());
        Assertions.assertFalse(error.type().shouldLog());
    }
}
