package io.quarkus.test.junit.mockito.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.inject.spi.BeanManager;

import org.mockito.Mockito;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.mockito.MockitoConfig;

public class CreateMockitoMocksCallback implements QuarkusTestAfterConstructCallback {

    @Override
    public void afterConstruct(Object testInstance) {
        Class<?> current = testInstance.getClass();
        while (current.getSuperclass() != null) {
            for (Field field : current.getDeclaredFields()) {
                InjectMock injectMock = field.getAnnotation(InjectMock.class);
                if (injectMock != null) {
                    MockitoConfig config = field.getAnnotation(MockitoConfig.class);
                    boolean returnsDeepMocks = config != null ? config.returnsDeepMocks() : false;
                    injectField(testInstance, field, InjectMock.class, returnsDeepMocks);
                }
            }
            current = current.getSuperclass();
        }
    }

    private void injectField(Object testInstance, Field field, Class<? extends Annotation> annotationType,
            boolean returnsDeepMocks) {
        InstanceHandle<?> beanHandle = getBeanHandle(testInstance, field, annotationType);
        Optional<Object> result = createMockAndSetTestField(testInstance, field, beanHandle,
                new MockConfiguration(returnsDeepMocks));
        if (result.isPresent()) {
            MockitoMocksTracker.track(testInstance, result.get(), beanHandle.get());
        }
    }

    private Optional<Object> createMockAndSetTestField(Object testInstance, Field field, InstanceHandle<?> beanHandle,
            MockConfiguration mockConfiguration) {
        Class<?> implementationClass = beanHandle.getBean().getImplementationClass();
        Object mock;
        boolean isNew;
        // Note that beanHandle.get() returns a client proxy for normal scoped beans; i.e. the contextual instance is not created
        Optional<Object> currentMock = MockitoMocksTracker.currentMock(testInstance, beanHandle.get());
        if (currentMock.isPresent()) {
            mock = currentMock.get();
            isNew = false;
        } else {
            if (mockConfiguration.useDeepMocks) {
                mock = Mockito.mock(implementationClass, Mockito.RETURNS_DEEP_STUBS);
            } else {
                mock = Mockito.mock(implementationClass);
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

    static InstanceHandle<?> getBeanHandle(Object testInstance, Field field, Class<? extends Annotation> annotationType) {
        Type fieldType = field.getGenericType();
        ArcContainer container = Arc.container();
        BeanManager beanManager = container.beanManager();
        Annotation[] qualifiers = getQualifiers(field, beanManager);

        InstanceHandle<?> handle = container.instance(fieldType, qualifiers);
        if (!handle.isAvailable()) {
            throw new IllegalStateException(
                    "Invalid use of " + annotationType.getTypeName() + " - could not resolve the bean of type: "
                            + fieldType.getTypeName() + ". Offending field is " + field.getName() + " of test class "
                            + testInstance.getClass());
        }
        if (!beanManager.isNormalScope(handle.getBean().getScope())) {
            throw new IllegalStateException(
                    "Invalid use of " + annotationType.getTypeName()
                            + " - the injected bean does not declare a CDI normal scope but: "
                            + handle.getBean().getScope().getName()
                            + ". Offending field is " + field.getName() + " of test class "
                            + testInstance.getClass());
        }
        return handle;
    }

    static Annotation[] getQualifiers(Field fieldToMock, BeanManager beanManager) {
        List<Annotation> qualifiers = new ArrayList<>();
        Annotation[] fieldAnnotations = fieldToMock.getDeclaredAnnotations();
        for (Annotation fieldAnnotation : fieldAnnotations) {
            if (beanManager.isQualifier(fieldAnnotation.annotationType())) {
                qualifiers.add(fieldAnnotation);
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
