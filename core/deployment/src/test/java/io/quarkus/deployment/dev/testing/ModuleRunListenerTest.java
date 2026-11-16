package io.quarkus.deployment.dev.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.TestIdentifier;

/**
 * The listener that aggregates the runs of the modules must forward every event to the registered listeners:
 * the total count shown by continuous testing relies on {@code dynamicTestRegistered} for parameterized tests.
 */
public class ModuleRunListenerTest {

    @Test
    public void everyEventIsForwarded() {
        List<String> events = new ArrayList<>();
        TestRunListener delegate = new TestRunListener() {
            @Override
            public void testComplete(TestResult result) {
                events.add("testComplete");
            }

            @Override
            public void runAborted() {
                events.add("runAborted");
            }

            @Override
            public void testStarted(TestIdentifier testIdentifier, String className) {
                events.add("testStarted " + className);
            }

            @Override
            public void dynamicTestRegistered(TestIdentifier testIdentifier) {
                events.add("dynamicTestRegistered");
            }
        };
        AtomicLong testCount = new AtomicLong();
        List<TestRunResults> allResults = new ArrayList<>();
        TestRunListener listener = new TestSupport.ModuleRunListener(testCount, List.of(delegate), allResults);

        listener.runStarted(3);
        listener.testStarted(null, "SomeTest");
        listener.dynamicTestRegistered(null);
        listener.testComplete(null);
        listener.runAborted();
        listener.runComplete(null);

        assertEquals(3, testCount.get());
        assertEquals(1, allResults.size());
        assertEquals(List.of("testStarted SomeTest", "dynamicTestRegistered", "testComplete", "runAborted"), events);
    }

    @Test
    public void everyListenerMethodIsOverridden() {
        for (Method method : TestRunListener.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.isSynthetic()) {
                continue;
            }
            boolean overridden = false;
            for (Method candidate : TestSupport.ModuleRunListener.class.getDeclaredMethods()) {
                if (candidate.getName().equals(method.getName())
                        && java.util.Arrays.equals(candidate.getParameterTypes(), method.getParameterTypes())) {
                    overridden = true;
                    break;
                }
            }
            assertTrue(overridden, "TestRunListener#" + method.getName() + " is not forwarded by ModuleRunListener");
        }
    }
}
