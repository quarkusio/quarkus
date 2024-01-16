package io.quarkus.deployment.dev;

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
                        new CompilerFlags(null, null, null, null, null, null),
                        new CompilerFlags(setOf(), listOf(), null, null, null, null)));
    }

    @Test
    void defaulting() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(setOf("-a", "-b"), listOf(), null, null, null, null),
                        new CompilerFlags(setOf(), listOf("-a", "-b"), null, null, null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf("-a", "-b"), listOf("-c", "-d"), null, null, null, null),
                        new CompilerFlags(setOf(), listOf("-a", "-b", "-c", "-d"), null, null, null, null)));
    }

    @Test
    void redundancyReduction() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(setOf("-a", "-b"), listOf(), null, null, null, null),
                        new CompilerFlags(setOf(), listOf("-a", "-b"), null, null, null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf("-a", "-b", "-c"), listOf("-a", "-b"), null, null, null, null),
                        new CompilerFlags(setOf("-c"), listOf("-a", "-b"), null, null, null, null)));
    }

    @Test
    void sourceAndTarget() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf(), "1", null, null, null),
                        new CompilerFlags(setOf(), listOf("--release", "1"), null, null, null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf(), null, "2", null, null),
                        new CompilerFlags(setOf(), listOf("-source", "2"), null, null, null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf(), null, null, "3", null),
                        new CompilerFlags(setOf(), listOf("-target", "3"), null, null, null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf(), "1", "2", "3", null),
                        new CompilerFlags(setOf(), listOf("--release", "1"), null, null, null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf(), null, "2", "3", null),
                        new CompilerFlags(setOf(), listOf("-source", "2", "-target", "3"), null, null, null, null)),
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf("-source", "5", "-target", "6"), null, "2", "3", null),
                        new CompilerFlags(setOf(), listOf("-source", "2", "-target", "3", "-source", "5", "-target", "6"),
                                null, null, null, null)));
    }

    @Test
    void allFeatures() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(setOf("-b", "-c", "-d"), listOf("-a", "-b", "-c"), "1", "2", "3", null),
                        new CompilerFlags(setOf(), listOf("-d", "--release", "1", "-a", "-b", "-c"), null, null, null, null)));
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(setOf("-b", "-c", "-d"), listOf("-a", "-b", "-c"), null, "2", "3", null),
                        new CompilerFlags(setOf(), listOf("-d", "-source", "2", "-target", "3", "-a", "-b", "-c"),
                                null, null, null, null)));
    }

    @Test
    void listConversion() {
        assertAll(
                () -> assertEquals(
                        new CompilerFlags(null, null, null, null, null, null).toList(),
                        listOf()),
                () -> assertEquals(
                        new CompilerFlags(setOf(), listOf("-a", "-b", "-c", "-d"), null, null, null, null).toList(),
                        listOf("-a", "-b", "-c", "-d")));
    }

    private List<String> listOf(String... strings) {
        return Arrays.asList(strings);
    }

    private Set<String> setOf(String... strings) {
        return new LinkedHashSet<>(Arrays.asList(strings));
    }

}
