package io.quarkus.test.junit.mockito.internal;

import java.lang.reflect.Field;

import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.mockito.InjectSpy;

public class CreateMockitoSpiesCallback implements QuarkusTestAfterConstructCallback {

    @Override
    public void afterConstruct(Object testInstance) {
        Class<?> current = testInstance.getClass();
        while (current.getSuperclass() != null) {
            for (Field field : current.getDeclaredFields()) {
                InjectSpy injectSpyAnnotation = field.getAnnotation(InjectSpy.class);
                if (injectSpyAnnotation != null) {
                    InstanceHandle<?> beanHandle = CreateMockitoMocksCallback.getBeanHandle(testInstance, field,
                            InjectSpy.class);
                    Object spy = createSpyAndSetTestField(testInstance, field, beanHandle,
                            injectSpyAnnotation.delegate());
                    MockitoMocksTracker.track(testInstance, spy, beanHandle.get());
                }
            }
            current = current.getSuperclass();
        }
    }

    private Object createSpyAndSetTestField(Object testInstance, Field field, InstanceHandle<?> beanHandle, boolean delegate) {
        Object spy;
        Object contextualInstance = ClientProxy.unwrap(beanHandle.get());
        if (delegate) {
            spy = Mockito.mock(beanHandle.getBean().getImplementationClass(),
                    AdditionalAnswers.delegatesTo(contextualInstance));
        } else {
            // Unwrap the client proxy if needed
            spy = Mockito.spy(contextualInstance);
        }
        field.setAccessible(true);
        try {
            field.set(testInstance, spy);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return spy;
    }

}
