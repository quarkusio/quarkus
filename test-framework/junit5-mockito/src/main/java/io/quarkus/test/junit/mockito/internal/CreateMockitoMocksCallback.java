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
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Subclass;
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
                    Object contextualReference = getContextualReference(testInstance, field, InjectMock.class);
                    Optional<Object> result = createMockAndSetTestField(testInstance, field, contextualReference,
                            new MockConfiguration(returnsDeepMocks));
                    if (result.isPresent()) {
                        MockitoMocksTracker.track(testInstance, result.get(), contextualReference);
                    }
                }
            }
            current = current.getSuperclass();
        }
    }

    private Optional<Object> createMockAndSetTestField(Object testInstance, Field field, Object contextualReference,
            MockConfiguration mockConfiguration) {
        Class<?> implementationClass = getImplementationClass(contextualReference);
        Object mock;
        boolean isNew;
        Optional<Object> currentMock = MockitoMocksTracker.currentMock(testInstance, contextualReference);
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

    /**
     * Contextual reference of a normal scoped bean is a client proxy.
     *
     * @param testInstance
     * @param field
     * @param annotationType
     * @return a contextual reference of a bean
     */
    static Object getContextualReference(Object testInstance, Field field, Class<? extends Annotation> annotationType) {
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
        return handle.get();
    }

    static Class<?> getImplementationClass(Object contextualReference) {
        // Unwrap the client proxy if needed
        Object contextualInstance = ClientProxy.unwrap(contextualReference);
        // If the contextual instance is an intercepted subclass then mock the extended implementation class
        return contextualInstance instanceof Subclass ? contextualInstance.getClass().getSuperclass()
                : contextualInstance.getClass();
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
