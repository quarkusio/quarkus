package io.quarkus.test.common;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestResourceManagerInjectorTest {

    @ParameterizedTest
    @ValueSource(classes = { UsingInjectorTest.class, UsingInjectorTest2.class })
    void testTestInjector(Class<?> clazz) {
        TestResourceManager manager = new TestResourceManager(clazz);
        manager.start();

        Foo foo = new Foo();
        manager.inject(foo);

        Assertions.assertNotNull(foo.bar);
        Assertions.assertEquals("bar", foo.bar.value);
        Assertions.assertNotNull(foo.dummy);
        Assertions.assertEquals("dummy", foo.dummy.value);
    }

    @WithTestResource(value = UsingTestInjectorLifecycleManager.class, scope = TestResourceScope.GLOBAL)
    public static class UsingInjectorTest {
    }

    @QuarkusTestResource(UsingTestInjectorLifecycleManager.class)
    public static class UsingInjectorTest2 {
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
