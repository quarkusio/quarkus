package io.quarkus.test.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class TestResourceManagerTest {

    @Test
    void testRepeatableAnnotationsAreIndexed() {
        AtomicInteger counter = new AtomicInteger();
        TestResourceManager manager = new TestResourceManager(MyTest.class);
        manager.inject(counter);
        assertThat(counter.intValue()).isEqualTo(2);
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
        public void inject(Object testInstance) {
            ((AtomicInteger) testInstance).incrementAndGet();
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
        public void inject(Object testInstance) {
            ((AtomicInteger) testInstance).incrementAndGet();
        }

        @Override
        public void stop() {

        }
    }

}