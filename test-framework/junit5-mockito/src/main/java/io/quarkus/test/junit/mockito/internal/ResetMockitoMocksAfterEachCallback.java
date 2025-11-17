package io.quarkus.test.junit.mockito.internal;

import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class ResetMockitoMocksAfterEachCallback implements QuarkusTestAfterEachCallback {

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        System.out.println("AfterEach ejecutando: ResetMockitoMocksAfterEachCallback");
        MockitoMocksTracker.reset(context.getTestInstance());
    }
}
