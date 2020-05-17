package io.quarkus.test.junit.mockito.internal;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;

public class SetMockitoMockAsBeanMockCallback implements QuarkusTestBeforeEachCallback {

    @Override
    public void beforeEach(Object testInstance) {
        MockitoMocksTracker.getMocks(testInstance).forEach(this::installMock);
    }

    private void installMock(MockitoMocksTracker.Mocked mocked) {
        QuarkusMock.installMockForInstance(mocked.mock, mocked.beanInstance);
    }
}