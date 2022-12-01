package io.quarkus.it.main;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
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
@QuarkusTestResource(value = QuarkusTestNestedWithResourcesTestCase.DummyTestResource.class, restrictToAnnotatedClass = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QuarkusTestNestedWithResourcesTestCase {

    public static final AtomicInteger COUNTER = new AtomicInteger(0);
    public static final AtomicInteger COUNT_RESOURCE_STARTS = new AtomicInteger(0);

    @InjectDummyString
    String bar;

    @Test
    @Order(1)
    public void testBarFromOuter() {
        Assertions.assertEquals("bar", bar);
        COUNTER.incrementAndGet();
    }

    @Test
    @Order(2)
    public void testResourceShouldNotHaveBeenRestarted() {
        Assertions.assertEquals(1, COUNT_RESOURCE_STARTS.get());
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class NestedTestClass {

        @Test
        @Order(1)
        public void testBarFromNested() {
            COUNTER.incrementAndGet();
            Assertions.assertEquals("bar", bar);
        }

        @Test
        @Order(2)
        public void testResourceShouldNotHaveBeenRestarted() {
            Assertions.assertEquals(2, COUNTER.get());
            Assertions.assertEquals(1, COUNT_RESOURCE_STARTS.get());
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface InjectDummyString {
    }

    public static class DummyTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            COUNT_RESOURCE_STARTS.incrementAndGet();
            return null;
        }

        @Override
        public void stop() {
        }

        @Override
        public void inject(TestInjector testInjector) {
            testInjector.injectIntoFields("bar",
                    new TestInjector.AnnotatedAndMatchesType(InjectDummyString.class, String.class));
        }
    }
}
