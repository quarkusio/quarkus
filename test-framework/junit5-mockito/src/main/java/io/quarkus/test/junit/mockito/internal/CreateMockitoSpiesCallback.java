package io.quarkus.test.junit.mockito.internal;

import java.lang.reflect.Field;

import org.mockito.Mockito;

import io.quarkus.arc.runtime.ClientProxyUnwrapper;
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
                    Object beanInstance = CreateMockitoMocksCallback.getBeanInstance(testInstance, field, InjectSpy.class);
                    Object spy = createSpyAndSetTestField(testInstance, field, beanInstance);
                    MockitoMocksTracker.track(testInstance, spy, beanInstance);
                }
            }
            current = current.getSuperclass();
        }
    }

    private Object createSpyAndSetTestField(Object testInstance, Field field, Object beanInstance) {
        ClientProxyUnwrapper unwrapper = new ClientProxyUnwrapper();
        Object spy = Mockito.spy(unwrapper.apply(beanInstance));
        field.setAccessible(true);
        try {
            field.set(testInstance, spy);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return spy;
    }

}
