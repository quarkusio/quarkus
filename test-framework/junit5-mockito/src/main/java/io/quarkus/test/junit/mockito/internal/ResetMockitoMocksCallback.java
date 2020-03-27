package io.quarkus.test.junit.mockito.internal;

import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;

public class ResetMockitoMocksCallback implements QuarkusTestAfterEachCallback {

    @Override
    public void afterEach(Object testInstance) {
        MockitoMocksTracker.reset(testInstance);
    }
}
