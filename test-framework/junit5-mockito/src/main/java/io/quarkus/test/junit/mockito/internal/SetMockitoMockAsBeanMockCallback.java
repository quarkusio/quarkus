package io.quarkus.test.junit.mockito.internal;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class SetMockitoMockAsBeanMockCallback implements QuarkusTestBeforeEachCallback {

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        MockitoMocksTracker.getMocks(context.getTestInstance()).forEach(this::installMock);
    }

    private void installMock(MockitoMocksTracker.Mocked mocked) {
        QuarkusMock.installMockForInstance(mocked.mock, mocked.beanInstance);
    }
}
