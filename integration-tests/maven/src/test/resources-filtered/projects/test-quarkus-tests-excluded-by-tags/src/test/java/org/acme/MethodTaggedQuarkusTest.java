package org.acme;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Only one of the two tests carries the "partial" tag: excluding that tag still leaves a test to run, so Quarkus
 * still has to build the application (which fails, see {@link BrokenBean}). Excluding the class-level tag excludes
 * both.
 */
@QuarkusTest
@Tag("quarkus-app")
public class MethodTaggedQuarkusTest {

    @Test
    @Tag("partial")
    void tagged() {
        fail("This test should never run");
    }

    @Test
    void untagged() {
        fail("This test should never run");
    }
}
