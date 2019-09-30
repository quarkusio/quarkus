package io.quarkus.test.junit.mockito.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mockito.Mockito;

import io.quarkus.arc.Arc;

public final class MocksTracker {

    final static Map<String, Set<String>> TEST_CLASS_TO_USED_MOCKS = new HashMap<>();

    private MocksTracker() {
    }

    public static void track(String testClass, String mockClass) {
        TEST_CLASS_TO_USED_MOCKS.computeIfAbsent(testClass, (k) -> new HashSet<>());
        TEST_CLASS_TO_USED_MOCKS.get(testClass).add(mockClass);
    }

    public static void reset(String testClass) {
        Object[] mocks = TEST_CLASS_TO_USED_MOCKS.get(testClass).stream().map(c -> {
            try {
                return Arc.container().instance(Class.forName(c)).get();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).toArray(Object[]::new);
        Mockito.reset(mocks);
    }
}
