package io.quarkus.test.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestResourceManagerTest {

    private static final String OVERRIDEN_KEY = "overridenKey";
    public static boolean parallelTestResourceRunned = false;

    @Test
    void testRepeatableAnnotationsAreIndexed() {
        AtomicInteger counter = new AtomicInteger();
        TestResourceManager manager = new TestResourceManager(MyTest.class);
        manager.inject(counter);
        assertThat(counter.intValue()).isEqualTo(2);
    }

    @Test
    void testSequentialResourcesRunSequentially() {
        TestResourceManager manager = new TestResourceManager(SequentialTestResourcesTest.class);
        Map<String, String> props = manager.start();
        Assertions.assertEquals("value1", props.get("key1"));
        Assertions.assertEquals("value2", props.get("key2"));
        Assertions.assertEquals("value2", props.get(OVERRIDEN_KEY));
    }

    @Test
    void testParallelResourcesRunInParallel() {
        TestResourceManager manager = new TestResourceManager(ParallelTestResourcesTest.class);
        Map<String, String> props = manager.start();
        Assertions.assertEquals("value1", props.get("key1"));
        Assertions.assertEquals("value2", props.get("key2"));
    }

    @QuarkusTestResource(FirstLifecycleManager.class)
    @QuarkusTestResource(SecondLifecycleManager.class)
    public static class MyTest {
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

    @QuarkusTestResource(FirstSequentialQuarkusTestResource.class)
    @QuarkusTestResource(SecondSequentialQuarkusTestResource.class)
    public static class SequentialTestResourcesTest {
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

    @QuarkusTestResource(value = FirstParallelQuarkusTestResource.class, parallel = true)
    @QuarkusTestResource(value = SecondParallelQuarkusTestResource.class, parallel = true)
    public static class ParallelTestResourcesTest {
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
}
