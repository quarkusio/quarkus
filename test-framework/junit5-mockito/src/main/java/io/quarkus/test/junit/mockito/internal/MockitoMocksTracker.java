package io.quarkus.test.junit.mockito.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

final class MockitoMocksTracker {

    final static Map<Object, Set<Mocked>> TEST_TO_USED_MOCKS = new ConcurrentHashMap<>();

    private MockitoMocksTracker() {
    }

    static void track(Object testInstance, Object mock, Object beanInstance) {
        TEST_TO_USED_MOCKS.computeIfAbsent(testInstance, k -> new HashSet<>()).add(new Mocked(mock, beanInstance));
    }

    static Set<Mocked> getMocks(Object testInstance) {
        return TEST_TO_USED_MOCKS.getOrDefault(testInstance, Collections.emptySet());
    }

    static void reset(Object testInstance) {
        for (Mocked m : getMocks(testInstance)) {
            MockUtil.resetMock(m.mock);
        }
    }

    static void clear(Object testInstance) {
        for (Mocked m : getMocks(testInstance)) {
            Mockito.framework().clearInlineMock(m.mock);
        }
        TEST_TO_USED_MOCKS.remove(testInstance);
    }

    static Optional<Object> currentMock(Object testInstance, Object beanInstance) {
        Set<Mocked> mocks = getMocks(testInstance);
        for (Mocked mocked : mocks) {
            if (mocked.beanInstance == beanInstance) {
                return Optional.of(mocked.mock);
            }
        }
        return Optional.empty();
    }

    // don't use a Record because we don't want the auto-generated methods which delegate to the components
    // see https://github.com/quarkusio/quarkus/issues/47739
    @SuppressWarnings("ClassCanBeRecord")
    final static class Mocked {

        final Object mock;
        final Object beanInstance;

        Mocked(Object mock, Object beanInstance) {
            this.mock = mock;
            this.beanInstance = beanInstance;
        }
    }
}
