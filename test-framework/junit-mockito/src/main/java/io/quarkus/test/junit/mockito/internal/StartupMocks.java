package io.quarkus.test.junit.mockito.internal;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import org.mockito.Mockito;

import io.quarkus.arc.runtime.MockRecorder;
import io.quarkus.arc.runtime.MockRecorder.Mock;
import io.quarkus.runtime.Startup;
import io.quarkus.test.junit.QuarkusMock;

@Startup(value = 1)
@Singleton
public class StartupMocks {
    @PostConstruct
    void onStart() {
        for (Mock mock : MockRecorder.beansToMock) {
            Class<?> mockType = mock.beanHandle().getBean().getImplementationClass();
            Object mockInstance = mock.deepMocks() ? Mockito.mock(mockType, RETURNS_DEEP_STUBS) : Mockito.mock(mockType);
            Object beanInstance = mock.beanHandle().get();
            QuarkusMock.installMockForInstance(mockInstance, beanInstance);
        }
    }
}
