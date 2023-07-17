package io.quarkus.test.component.declarative;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.NoClassInterceptors;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.beans.Charlie;

@QuarkusComponentTest
public class InterceptorMethodsTest {

    static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    @Inject
    TheComponent theComponent;

    // Charlie is mocked even if it's not a dependency of a tested component
    @InjectMock
    Charlie charlie;

    @Test
    public void testPing() {
        EVENTS.clear();
        Mockito.when(charlie.ping()).thenReturn("ok");
        assertEquals("OK", theComponent.ping());
        Arc.container().getActiveContext(ApplicationScoped.class).destroy(theComponent.getBean());
        assertEquals(5, EVENTS.size());
        assertEquals("ac", EVENTS.get(0));
        assertEquals("pc", EVENTS.get(1));
        assertEquals("ai2", EVENTS.get(2));
        assertEquals("ai1", EVENTS.get(3));
        assertEquals("pd", EVENTS.get(4));
    }

    @SimpleBinding
    @ApplicationScoped
    static class TheComponent {

        @Inject
        Bean<TheComponent> bean;

        String ping() {
            return "true";
        }

        @NoClassInterceptors
        public Bean<?> getBean() {
            return bean;
        }

    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @InterceptorBinding
    public @interface SimpleBinding {

    }

    @Priority(20)
    @SimpleBinding
    @AroundInvoke
    Object aroundInvoke1(InvocationContext context) throws Exception {
        EVENTS.add("ai1");
        return Boolean.parseBoolean(context.proceed().toString()) ? charlie.ping() : "false";
    }

    // default priority is 1
    @SimpleBinding
    @AroundInvoke
    Object aroundInvoke2(InvocationContext context) throws Exception {
        EVENTS.add("ai2");
        return context.proceed().toString().toUpperCase();
    }

    @SimpleBinding
    @PostConstruct
    void postConstruct(ArcInvocationContext context) throws Exception {
        EVENTS.add("pc");
        context.proceed();
    }

    @SimpleBinding
    @PreDestroy
    void preDestroy(ArcInvocationContext context) throws Exception {
        EVENTS.add("pd");
        context.proceed();
    }

    @SimpleBinding
    @AroundConstruct
    void aroundConstruct(ArcInvocationContext context) throws Exception {
        EVENTS.add("ac");
        context.proceed();
    }

}
