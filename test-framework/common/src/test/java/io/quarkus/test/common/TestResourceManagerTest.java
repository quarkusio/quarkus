package io.quarkus.test.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestResourceManagerTest {

    private static final String OVERRIDEN_KEY = "overridenKey";
    public static boolean parallelTestResourceRunned = false;

    @ParameterizedTest
    @ValueSource(classes = { MyTest.class, MyTest2.class })
    void testRepeatableAnnotationsAreIndexed(Class<?> clazz) {
        AtomicInteger counter = new AtomicInteger();
        TestResourceManager manager = new TestResourceManager(clazz);
        manager.inject(counter);
        assertThat(counter.intValue()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(classes = { SequentialTestResourcesTest.class, SequentialTestResourcesTest2.class })
    void testSequentialResourcesRunSequentially(Class<?> clazz) {
        TestResourceManager manager = new TestResourceManager(clazz);
        Map<String, String> props = manager.start();
        Assertions.assertEquals("value1", props.get("key1"));
        Assertions.assertEquals("value2", props.get("key2"));
        Assertions.assertEquals("value2", props.get(OVERRIDEN_KEY));
    }

    @ParameterizedTest
    @ValueSource(classes = { ParallelTestResourcesTest.class, ParallelTestResourcesTest2.class })
    void testParallelResourcesRunInParallel(Class<?> clazz) {
        TestResourceManager manager = new TestResourceManager(clazz);
        Map<String, String> props = manager.start();
        Assertions.assertEquals("value1", props.get("key1"));
        Assertions.assertEquals("value2", props.get("key2"));
    }

    @WithTestResource(value = FirstLifecycleManager.class, scope = TestResourceScope.GLOBAL)
    @WithTestResource(value = SecondLifecycleManager.class, scope = TestResourceScope.GLOBAL)
    public static class MyTest {
    }

    @QuarkusTestResource(FirstLifecycleManager.class)
    @QuarkusTestResource(SecondLifecycleManager.class)
    public static class MyTest2 {
    }

    public static class FirstLifecycleManager implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Collections.emptyMap();
        }

        @Override
        public void inject(Object instance) {
            if (instance instanceof AtomicInteger) {
                ((AtomicInteger) instance).incrementAndGet();
            }
        }

        @Override
        public void stop() {

        }
    }

    public static class SecondLifecycleManager implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Collections.emptyMap();
        }

        @Override
        public void inject(Object instance) {
            if (instance instanceof AtomicInteger) {
                ((AtomicInteger) instance).incrementAndGet();
            }
        }

        @Override
        public void stop() {

        }
    }

    @WithTestResource(value = FirstSequentialQuarkusTestResource.class, scope = TestResourceScope.GLOBAL)
    @WithTestResource(value = SecondSequentialQuarkusTestResource.class, scope = TestResourceScope.GLOBAL)
    public static class SequentialTestResourcesTest {
    }

    @QuarkusTestResource(FirstSequentialQuarkusTestResource.class)
    @QuarkusTestResource(SecondSequentialQuarkusTestResource.class)
    public static class SequentialTestResourcesTest2 {
    }

    public static class FirstSequentialQuarkusTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>();
            props.put("key1", "value1");
            props.put(OVERRIDEN_KEY, "value1");
            return props;
        }

        @Override
        public void stop() {
        }

        @Override
        public int order() {
            return 1;
        }
    }

    public static class SecondSequentialQuarkusTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>();
            props.put("key2", "value2");
            props.put(OVERRIDEN_KEY, "value2");
            return props;
        }

        @Override
        public void stop() {

        }

        @Override
        public int order() {
            return 2;
        }
    }

    @WithTestResource(value = FirstParallelQuarkusTestResource.class, parallel = true, scope = TestResourceScope.GLOBAL)
    @WithTestResource(value = SecondParallelQuarkusTestResource.class, parallel = true, scope = TestResourceScope.GLOBAL)
    public static class ParallelTestResourcesTest {
    }

    @QuarkusTestResource(value = FirstParallelQuarkusTestResource.class, parallel = true)
    @QuarkusTestResource(value = SecondParallelQuarkusTestResource.class, parallel = true)
    public static class ParallelTestResourcesTest2 {
    }

    public static class FirstParallelQuarkusTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            try {
                // sleep so the SecondParallelQuarkusTestResource finishes, incrementing the parallel counter first
                Thread.sleep(25);
                Assertions.assertTrue(parallelTestResourceRunned, "The SecondParallelQuarkusTestResource did not run yet!");
                return Collections.singletonMap("key1", "value1");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void stop() {
        }

        @Override
        public int order() {
            return 1;
        }
    }

    public static class SecondParallelQuarkusTestResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            parallelTestResourceRunned = true;
            return Collections.singletonMap("key2", "value2");
        }

        @Override
        public void stop() {
        }

        @Override
        public int order() {
            return 2;
        }
    }

    @ParameterizedTest
    @ValueSource(classes = { RepeatableAnnotationBasedTestResourcesTest.class,
            RepeatableAnnotationBasedTestResourcesTest2.class })
    void testAnnotationBased(Class<?> clazz) {
        TestResourceManager manager = new TestResourceManager(clazz);
        manager.init("test");
        Map<String, String> props = manager.start();
        Assertions.assertEquals("value", props.get("annotationkey1"));
        Assertions.assertEquals("value", props.get("annotationkey2"));
    }

    public static class AnnotationBasedQuarkusTestResource
            implements QuarkusTestResourceConfigurableLifecycleManager<WithAnnotationBasedTestResource> {

        private String key;

        @Override
        public void init(WithAnnotationBasedTestResource annotation) {
            this.key = annotation.key();
        }

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>();
            props.put(key, "value");
            return props;
        }

        @Override
        public void stop() {
        }
    }

    public static class AnnotationBasedQuarkusTestResource2
            implements QuarkusTestResourceConfigurableLifecycleManager<WithAnnotationBasedTestResource2> {

        private String key;

        @Override
        public void init(WithAnnotationBasedTestResource2 annotation) {
            this.key = annotation.key();
        }

        @Override
        public Map<String, String> start() {
            Map<String, String> props = new HashMap<>();
            props.put(key, "value");
            return props;
        }

        @Override
        public void stop() {
        }
    }

    @WithTestResource(value = AnnotationBasedQuarkusTestResource.class, scope = TestResourceScope.GLOBAL)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(WithAnnotationBasedTestResource.List.class)
    public @interface WithAnnotationBasedTestResource {
        String key() default "";

        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.RUNTIME)
        @WithTestResourceRepeatable(WithAnnotationBasedTestResource.class)
        @interface List {
            WithAnnotationBasedTestResource[] value();
        }
    }

    @QuarkusTestResource(AnnotationBasedQuarkusTestResource2.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(WithAnnotationBasedTestResource2.List.class)
    public @interface WithAnnotationBasedTestResource2 {
        String key() default "";

        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.RUNTIME)
        @QuarkusTestResourceRepeatable(WithAnnotationBasedTestResource2.class)
        @interface List {
            WithAnnotationBasedTestResource2[] value();
        }
    }

    @WithAnnotationBasedTestResource(key = "annotationkey1")
    @WithAnnotationBasedTestResource(key = "annotationkey2")
    public static class RepeatableAnnotationBasedTestResourcesTest {
    }

    @WithAnnotationBasedTestResource2(key = "annotationkey1")
    @WithAnnotationBasedTestResource2(key = "annotationkey2")
    public static class RepeatableAnnotationBasedTestResourcesTest2 {
    }
}
