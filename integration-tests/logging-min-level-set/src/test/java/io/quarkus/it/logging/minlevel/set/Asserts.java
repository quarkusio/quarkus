package io.quarkus.it.logging.minlevel.set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiConsumer;

final class Asserts {
    private static boolean assertLogged(boolean expected, String msg, BiConsumer<String, Throwable> logFunction) {
        try (AssertThrowable t = new AssertThrowable(expected)) {
            logFunction.accept(msg, t);
            if (t.expected)
                return t.executed;
            else
                return t.executed;
        }
    }

    private static final class AssertThrowable extends Throwable implements AutoCloseable {

        final boolean expected;
        boolean executed;

        private AssertThrowable(boolean expected) {
            this.expected = expected;
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            executed = true;
            return new StackTraceElement[] {};
        }

        @Override
        public void close() {
            if (expected) {
                assertTrue(executed, "Expected message to be printed but didn't get printed");
            } else {
                assertFalse(executed, "Expected message not to be printed but got printed");
            }
        }
    }

    private Asserts() {
    }
}
