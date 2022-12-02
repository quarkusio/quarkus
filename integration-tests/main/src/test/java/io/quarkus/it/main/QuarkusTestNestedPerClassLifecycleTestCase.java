package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests {@link Nested} support of {@link QuarkusTest}. Notes:
 * <ul>
 * <li>to avoid unexpected execution order, don't use surefire's {@code -Dtest=...}, use {@code -Dgroups=nested} instead</li>
 * <li>order of nested test classes is reversed by JUnit (and there's no way to enforce a specific order)</li>
 * </ul>
 */
@QuarkusTest
@Tag("nested")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QuarkusTestNestedPerClassLifecycleTestCase {

    private final AtomicInteger counter = new AtomicInteger(0);

    @BeforeAll
    public void incrementInBeforeAll() {
        counter.incrementAndGet();
    }

    /**
     * We're doing nothing with this code, but we want to keep it to verify the methods annotated
     * with `@AfterAll` work for nested tests.
     */
    @AfterAll
    public void incrementInAfterAll() {
        counter.incrementAndGet();
    }

    @Nested
    class NestedTest {

        @Test
        public void verifyCounter() {
            assertEquals(2, counter.incrementAndGet());
        }
    }
}
