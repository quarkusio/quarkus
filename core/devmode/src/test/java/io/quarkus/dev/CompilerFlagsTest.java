package io.quarkus.dev;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class CompilerFlagsTest {

    @Test
    void nullHandling() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(null, null, null, null),
                        new CompilerFlags(setOf(), listOf(), null, null)));
    }

    @Test
    void defaulting() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(setOf("-a", "-b"), listOf(), null, null),
                        new CompilerFlags(setOf(), listOf("-a", "-b"), null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf("-a", "-b"), listOf("-c", "-d"), null, null),
                        new CompilerFlags(setOf(), listOf("-a", "-b", "-c", "-d"), null, null)));
    }

    @Test
    void redundancyReduction() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(setOf("-a", "-b"), listOf(), null, null),
                        new CompilerFlags(setOf(), listOf("-a", "-b"), null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf("-a", "-b", "-c"), listOf("-a", "-b"), null, null),
                        new CompilerFlags(setOf("-c"), listOf("-a", "-b"), null, null)));
    }

    @Test
    void sourceAndTarget() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf(), "1", null),
                        new CompilerFlags(setOf(), listOf("-source", "1"), null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf(), null, "2"),
                        new CompilerFlags(setOf(), listOf("-target", "2"), null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf(), "1", "2"),
                        new CompilerFlags(setOf(), listOf("-source", "1", "-target", "2"), null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf("-source", "3", "-target", "4"), "1", "2"),
                        new CompilerFlags(setOf(), listOf("-source", "1", "-target", "2", "-source", "3", "-target", "4"), null,
                                null)));
    }

    @Test
    void allFeatures() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(setOf("-b", "-c", "-d"), listOf("-a", "-b", "-c"), "1", "2"),
                        new CompilerFlags(setOf(), listOf("-d", "-source", "1", "-target", "2", "-a", "-b", "-c"), null,
                                null)));
    }

    @Test
    void listConversion() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(null, null, null, null).toList(),
                        listOf()),
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf("-a", "-b", "-c", "-d"), null, null).toList(),
                        listOf("-a", "-b", "-c", "-d")));
    }

    private List<String> listOf(String... strings) {
        return Arrays.asList(strings);
    }

    private Set<String> setOf(String... strings) {
        return new LinkedHashSet<>(Arrays.asList(strings));
    }

}
