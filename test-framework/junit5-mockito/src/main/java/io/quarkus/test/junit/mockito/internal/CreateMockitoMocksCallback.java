package io.quarkus.test.junit.mockito.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Qualifier;

import org.mockito.Mockito;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
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
                    boolean returnsDeepMocks = injectMockAnnotation.returnsDeepMocks();
                    Object beanInstance = getBeanInstance(testInstance, field, InjectMock.class);
                    Optional<Object> result = createMockAndSetTestField(testInstance, field, beanInstance,
                            new MockConfiguration(returnsDeepMocks));
                    if (result.isPresent()) {
                        MockitoMocksTracker.track(testInstance, result.get(), beanInstance);
                    }
                }
            }
            current = current.getSuperclass();
        }
    }

    private Optional<Object> createMockAndSetTestField(Object testInstance, Field field, Object beanInstance,
            MockConfiguration mockConfiguration) {
        Class<?> beanClass = beanInstance.getClass();
        // make sure we don't mock proxy classes, especially given that they don't have generics info
        if (ClientProxy.class.isAssignableFrom(beanClass)) {
            // and yet some of them appear to have Object as supertype, avoid them
            if (beanClass.getSuperclass() != Object.class)
                beanClass = beanClass.getSuperclass();
        }
        Object mock;
        boolean isNew;
        Optional<Object> currentMock = MockitoMocksTracker.currentMock(testInstance, beanInstance);
        if (currentMock.isPresent()) {
            mock = currentMock.get();
            isNew = false;
        } else {
            if (mockConfiguration.useDeepMocks) {
                mock = Mockito.mock(beanClass, Mockito.RETURNS_DEEP_STUBS);
            } else {
                mock = Mockito.mock(beanClass);
            }
            isNew = true;
        }
        field.setAccessible(true);
        try {
            field.set(testInstance, mock);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (isNew) {
            return Optional.of(mock);
        } else {
            return Optional.empty();
        }
    }

    static Object getBeanInstance(Object testInstance, Field field, Class<? extends Annotation> annotationType) {
        Type fieldType = field.getGenericType();
        Annotation[] qualifiers = getQualifiers(field);
        ArcContainer container = Arc.container();
        BeanManager beanManager = container.beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(fieldType, qualifiers);
        if (beans.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid use of " + annotationType.getTypeName() + " - could not resolve the bean of type: "
                            + fieldType.getTypeName() + ". Offending field is " + field.getName() + " of test class "
                            + testInstance.getClass());
        }
        Bean<?> bean = beanManager.resolve(beans);
        if (!beanManager.isNormalScope(bean.getScope())) {
            throw new IllegalStateException(
                    "Invalid use of " + annotationType.getTypeName()
                            + " - the injected bean does not declare a CDI normal scope but: " + bean.getScope().getName()
                            + ". Offending field is " + field.getName() + " of test class "
                            + testInstance.getClass());
        }
        return container.instance((InjectableBean<?>) bean).get();
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

    private static class MockConfiguration {
        final boolean useDeepMocks;

        private MockConfiguration(boolean useDeepMocks) {
            this.useDeepMocks = useDeepMocks;
        }

    }
}
