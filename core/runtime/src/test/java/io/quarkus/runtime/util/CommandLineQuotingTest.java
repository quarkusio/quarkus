package io.quarkus.runtime.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CommandLineQuotingTest {

    @ParameterizedTest
    @MethodSource("arguments")
    void quote(String argument, String expected) {
        assertThat(CommandLineQuoting.quote(argument)).isEqualTo(expected);
        assertThat(CommandLineQuoting.quoteForArgumentFile(argument)).isEqualTo(expected);
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of("", "\"\""),
                Arguments.of("hello", "\"hello\""),
                Arguments.of("hello world", "\"hello world\""),
                Arguments.of("it's quoted", "\"it's quoted\""),
                Arguments.of("say \"hello\"", "\"say \\\"hello\\\"\""),
                Arguments.of("C:\\Program Files\\", "\"C:\\\\Program Files\\\\\""),
                Arguments.of("first\nsecond", "\"first\\nsecond\""),
                Arguments.of("literal\\n", "\"literal\\\\n\""));
    }

    @Test
    void argumentFileControlCharacters() {
        assertThat(CommandLineQuoting.quoteForArgumentFile("a\rb\tc\fd"))
                .isEqualTo("\"a\\rb\\tc\\fd\"");
        // Preserve the existing Aesh escaping rules; its parser is not the argument-file parser.
        assertThat(CommandLineQuoting.quote("a\rb\tc\fd"))
                .isEqualTo("\"a\rb\tc\fd\"");
    }
}
