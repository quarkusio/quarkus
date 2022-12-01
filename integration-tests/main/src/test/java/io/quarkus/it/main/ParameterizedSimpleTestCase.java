package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ParameterizedSimpleTestCase {
    @ParameterizedTest
    @NullSource
    public void nullArgument(String arg) {
        assertNull(arg);
    }

    @ParameterizedTest
    @EmptySource
    public void emptyArgument(String arg) {
        assertEquals("", arg);
    }

    @ParameterizedTest
    @ValueSource(strings = { "foobar" })
    public void nonemptyArgument(String arg) {
        assertEquals("foobar", arg);
    }
}
