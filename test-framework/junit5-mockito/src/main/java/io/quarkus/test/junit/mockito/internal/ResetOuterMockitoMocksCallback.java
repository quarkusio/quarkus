package io.quarkus.test.junit.mockito.internal;

import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;

public class ResetOuterMockitoMocksCallback implements QuarkusTestAfterAllCallback {

    @Override
    public void afterAll(QuarkusTestContext context) {
        if (context.getOuterInstance() != null) {
            MockitoMocksTracker.reset(context.getOuterInstance());
        }
    }
}
