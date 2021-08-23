package io.quarkus.test.junit.mockito.internal;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class SetMockitoMockAsBeanMockCallback implements QuarkusTestBeforeEachCallback {

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        MockitoMocksTracker.getMocks(context.getTestInstance()).forEach(this::installMock);
        if (context.getOuterInstance() != null) {
            MockitoMocksTracker.getMocks(context.getOuterInstance()).forEach(this::installMock);
        }
    }

    private void installMock(MockitoMocksTracker.Mocked mocked) {
        try {
            QuarkusMock.installMockForInstance(mocked.mock, mocked.beanInstance);
        } catch (Exception e) {
            throw new RuntimeException(mocked.beanInstance
                    + " is not a normal scoped CDI bean, make sure the bean is a normal scope like @ApplicationScoped or @RequestScoped."
                    + " Alternatively you can use '@InjectMock(convertScopes=true)' instead of '@InjectMock' if you would like"
                    + " Quarkus to automatically make that conversion (you should only use this if you understand the implications).");
        }
    }
}
