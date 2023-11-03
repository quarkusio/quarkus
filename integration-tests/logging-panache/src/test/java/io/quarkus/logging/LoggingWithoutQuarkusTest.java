package io.quarkus.logging;

import org.junit.jupiter.api.Test;

public class LoggingWithoutQuarkusTest {
    @Test
    public void test() {
        Log.error("This should not fail, we're in a test");
    }
}
