package org.acme;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * A plain JUnit test, to show that the run itself is not empty when the Quarkus tests are excluded.
 */
@Tag("vanilla")
public class VanillaTest {

    @Test
    void runs() {
        assertTrue(true);
    }
}
