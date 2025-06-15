package io.quarkus.test.junit.mockito.internal;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.mockito.InjectSpy;

public class CreateMockitoSpiesCallback implements QuarkusTestAfterConstructCallback, QuarkusTestAfterAllCallback {

    // Set is here because in nested tests, there are multiple states created before destruction is triggered
    // This field needs to be static because each implemented callback created a new instance of this class
    private static Set<InjectableContext.ContextState> statesToDestroy = new HashSet<>();

    @Override
    public void afterConstruct(Object testInstance) {
        Class<?> current = testInstance.getClass();
        // QuarkusTestAfterConstructCallback can be used in @QuarkusIntegrationTest where there is no Arc
        ArcContainer container = Arc.container();
        boolean contextPreviouslyActive = container != null && container.requestContext().isActive();
        if (!contextPreviouslyActive) {
            statesToDestroy.add(container.requestContext().activate());
        }
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
        if (!contextPreviouslyActive) {
            // only deactivate; we will destroy them in QuarkusTestAfterAllCallback
            container.requestContext().deactivate();
        }
    }

    private Object createSpyAndSetTestField(Object testInstance, Field field, InstanceHandle<?> beanHandle,
            boolean delegate) {
        Object spy;
        // Unwrap the client proxy if needed
        Object contextualInstance = ClientProxy.unwrap(beanHandle.get());
        if (delegate) {
            spy = Mockito.mock(beanHandle.getBean().getImplementationClass(),
                    AdditionalAnswers.delegatesTo(contextualInstance));
        } else {
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

    @Override
    public void afterAll(QuarkusTestContext context) {
        if (!statesToDestroy.isEmpty()) {
            for (InjectableContext.ContextState state : statesToDestroy) {
                Arc.container().requestContext().destroy(state);
            }
            statesToDestroy.clear();
        }
    }
}
