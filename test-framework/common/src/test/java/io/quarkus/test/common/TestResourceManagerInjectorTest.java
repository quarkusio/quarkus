package io.quarkus.test.common;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestResourceManagerInjectorTest {

    @Test
    void testTestInjector() {
        TestResourceManager manager = new TestResourceManager(UsingInjectorTest.class);
        manager.start();

        Foo foo = new Foo();
        manager.inject(foo);

        Assertions.assertNotNull(foo.bar);
        Assertions.assertEquals("bar", foo.bar.value);
        Assertions.assertNotNull(foo.dummy);
        Assertions.assertEquals("dummy", foo.dummy.value);
    }

    @QuarkusTestResource(UsingTestInjectorLifecycleManager.class)
    public static class UsingInjectorTest {
    }

    public static class UsingTestInjectorLifecycleManager implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Collections.emptyMap();
        }

        @Override
        public void stop() {

        }

        @Override
        public void inject(TestInjector testInjector) {
            TestResourceManager.DefaultTestInjector defaultTestInjector = (TestResourceManager.DefaultTestInjector) testInjector;
            if (defaultTestInjector.testInstance instanceof Foo) {
                testInjector.injectIntoFields(new Bar("bar"), (f) -> f.getType().isAssignableFrom(BarBase.class));
                testInjector.injectIntoFields(new Dummy("dummy"), (f) -> f.getType().equals(Dummy.class));
            }
        }
    }

    public static abstract class BarBase {
        public final String value;

        public BarBase(String value) {
            this.value = value;
        }
    }

    public static class Bar extends BarBase {
        public Bar(String value) {
            super(value);
        }
    }

    public static class Dummy {
        public final String value;

        public Dummy(String value) {
            this.value = value;
        }
    }

    public static class Foo {
        BarBase bar;
        @InjectDummy
        Dummy dummy;
    }

    @Target({ FIELD })
    @Retention(RUNTIME)
    public @interface InjectDummy {
    }
}
