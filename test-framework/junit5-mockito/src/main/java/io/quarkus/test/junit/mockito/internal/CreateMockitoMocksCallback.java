package io.quarkus.test.junit.mockito.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Qualifier;

import org.mockito.Mockito;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.mockito.InjectMock;

public class CreateMockitoMocksCallback implements QuarkusTestAfterConstructCallback {

    @Override
    public void afterConstruct(Object testInstance) {
        Class<?> current = testInstance.getClass();
        while (current.getSuperclass() != null) {
            for (Field field : current.getDeclaredFields()) {
                InjectMock injectMockAnnotation = field.getAnnotation(InjectMock.class);
                if (injectMockAnnotation != null) {
                    Object beanInstance = getBeanInstance(testInstance, field, InjectMock.class);
                    Object mock = createMockAndSetTestField(testInstance, field, beanInstance);
                    MockitoMocksTracker.track(testInstance, mock, beanInstance);
                }
            }
            current = current.getSuperclass();
        }
    }

    private Object createMockAndSetTestField(Object testInstance, Field field, Object beanInstance) {
        Class<?> beanClass = beanInstance.getClass();
        // make sure we don't mock proxy classes, especially given that they don't have generics info
        if (ClientProxy.class.isAssignableFrom(beanClass)) {
            // and yet some of them appear to have Object as supertype, avoid them
            if (beanClass.getSuperclass() != Object.class)
                beanClass = beanClass.getSuperclass();
        }
        Object mock = Mockito.mock(beanClass);
        field.setAccessible(true);
        try {
            field.set(testInstance, mock);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return mock;
    }

    static Object getBeanInstance(Object testInstance, Field field, Class<? extends Annotation> annotationType) {
        Class<?> fieldClass = field.getType();
        InstanceHandle<?> instance = Arc.container().instance(fieldClass, getQualifiers(field));
        if (!instance.isAvailable()) {
            throw new IllegalStateException(
                    "Invalid use of " + annotationType.getTypeName() + " - could not determine bean of type: "
                            + fieldClass + ". Offending field is " + field.getName() + " of test class "
                            + testInstance.getClass());
        }
        return instance.get();
    }

    static Annotation[] getQualifiers(Field fieldToMock) {
        List<Annotation> qualifiers = new ArrayList<>();
        Annotation[] fieldAnnotations = fieldToMock.getDeclaredAnnotations();
        for (Annotation fieldAnnotation : fieldAnnotations) {
            for (Annotation annotationOfFieldAnnotation : fieldAnnotation.annotationType().getAnnotations()) {
                if (annotationOfFieldAnnotation.annotationType().equals(Qualifier.class)) {
                    qualifiers.add(fieldAnnotation);
                    break;
                }
            }
        }
        return qualifiers.toArray(new Annotation[0]);
    }
}
