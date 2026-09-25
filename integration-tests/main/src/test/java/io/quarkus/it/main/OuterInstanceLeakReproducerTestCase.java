package io.quarkus.it.main;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Reproducer for the stale entry left in {@code QuarkusTestContext#getOuterInstances()} when an intermediate
 * {@link Nested} class has both its own {@link Test} and a deeper nested child.
 * <p>
 * The assertions are in {@link OuterInstanceLeakCheckerAfterAllCallback}, because the sizes are only observable from
 * an {@code afterAll} callback. Run with {@code ./mvnw test -f integration-tests/main -Dgroups=nested}.
 */
@QuarkusTest
@Tag("nested")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OuterInstanceLeakReproducerTestCase {

    @Test
    public void outer() {
    }

    @Nested
    class Middle {

        @Test
        public void middle() {
        }

        @Nested
        class Inner {

            @Test
            public void inner() {
            }
        }
    }
}
