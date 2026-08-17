package org.acme;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Excluded as a whole through its class-level tag; the nested test inherits that tag.
 * The application cannot be built (see {@link BrokenBean}), so this project only builds if no application is
 * created for this class.
 */
@QuarkusTest
@Tag("quarkus-app")
public class ClassTaggedQuarkusTest {

    @Test
    void neverRuns() {
        fail("This test is excluded by its class-level tag and should never run");
    }

    @Nested
    class Inner {

        @Test
        void neverRunsEither() {
            fail("This nested test inherits the class-level tag and should never run");
        }
    }
}
