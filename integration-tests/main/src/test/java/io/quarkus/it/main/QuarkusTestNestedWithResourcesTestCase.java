package io.quarkus.it.main;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
public class QuarkusTestNestedWithResourcesTestCase {

    @InjectDummyString
    String bar;

    @Test
    public void testBarFromOuter() {
        Assertions.assertEquals("bar", bar);
    }

    @Nested
    class NestedTestClass {

        @Test
        public void testBarFromNested() {
            Assertions.assertEquals("bar", bar);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface InjectDummyString {
    }

    public static class DummyTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
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
