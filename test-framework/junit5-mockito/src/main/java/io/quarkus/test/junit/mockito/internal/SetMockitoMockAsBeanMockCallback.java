package io.quarkus.test.junit.mockito.internal;

import java.lang.reflect.Method;

import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;

public class SetMockitoMockAsBeanMockCallback implements QuarkusTestBeforeEachCallback {

    private volatile Method installMocksMethod;

    @Override
    public void beforeEach(Object testInstance) {
        MockitoMocksTracker.getMocks(testInstance).forEach(m -> {
            installMocks(m, m.beanInstance);
        });
    }

    // call MockSupport.installMock using reflection since it is not public
    private void installMocks(MockitoMocksTracker.Mocked m, Object beanInstance) {
        try {
            if (installMocksMethod == null) {
                installMocksMethod = Class.forName("io.quarkus.test.junit.MockSupport").getDeclaredMethod("installMock",
                        Object.class,
                        Object.class);
                installMocksMethod.setAccessible(true);
            }
            installMocksMethod.invoke(null, beanInstance, m.mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
