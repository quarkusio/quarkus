package io.quarkus.test.junit.mockito.internal;

import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;

public class ResetMockitoMocksAfterAllCallback implements QuarkusTestAfterAllCallback {

    @Override
    public void afterAll(QuarkusTestContext context) {
        if (context.getTestInstance() != null) {
            MockitoMocksTracker.clear(context.getTestInstance());
        }

        if (context.getOuterInstances() != null) {
            for (Object outerInstance : context.getOuterInstances()) {
                MockitoMocksTracker.reset(outerInstance);
                MockitoMocksTracker.clear(outerInstance);
            }
        }

    }
}
