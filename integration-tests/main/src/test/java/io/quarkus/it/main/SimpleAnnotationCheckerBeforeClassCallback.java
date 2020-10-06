package io.quarkus.it.main;

import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.test.junit.callback.QuarkusTestBeforeClassCallback;

public class SimpleAnnotationCheckerBeforeClassCallback implements QuarkusTestBeforeClassCallback {

    public static AtomicInteger count = new AtomicInteger(0);

    @Override
    public void beforeClass(Class<?> testClass) {
        // make sure that this comes into play only for the test we care about
        if (!testClass.getName().endsWith("QuarkusTestCallbacksTestCase")) {
            return;
        }

        count.incrementAndGet();
    }
}
