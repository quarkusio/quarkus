package io.quarkus.it.main;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests {@link Nested} support of {@link QuarkusTest} with two levels of nesting and the per-class lifecycle.
 * Notes:
 * <ul>
 * <li>to avoid unexpected execution order, don't use surefire's {@code -Dtest=...}, use {@code -Dgroups=nested} instead</li>
 * </ul>
 */
@QuarkusTest
@Tag("nested")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QuarkusTestNestedTwoLevelsPerClassLifecycleTestCase {

    @BeforeAll
    public void beforeAll() {
        assertThat(this).isExactlyInstanceOf(QuarkusTestNestedTwoLevelsPerClassLifecycleTestCase.class);
    }

    /**
     * With two levels of nesting, the outer instance used to be swapped with the intermediate one,
     * so this method was invoked on a {@link LevelOne} instance.
     */
    @AfterAll
    public void afterAll() {
        assertThat(this).isExactlyInstanceOf(QuarkusTestNestedTwoLevelsPerClassLifecycleTestCase.class);
    }

    @Nested
    class LevelOne {

        @Nested
        class LevelTwo {

            @Test
            public void test() {
                assertThat(LevelOne.this).isExactlyInstanceOf(LevelOne.class);
            }
        }
    }
}
