package io.quarkus.test.component.declarative;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.beans.Charlie;

@QuarkusComponentTest
public class InterceptorMockingTest {

    @Inject
    TheComponent theComponent;

    @InjectMock
    Charlie charlie;

    @Test
    public void testPing() {
        Mockito.when(charlie.ping()).thenReturn("ok");
        assertEquals("ok", theComponent.ping());
    }

    @Singleton
    static class TheComponent {

        @SimpleBinding
        String ping() {
            return "true";
        }

    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @InterceptorBinding
    public @interface SimpleBinding {

    }

    // This interceptor is automatically added as a tested component
    @Priority(1)
    @SimpleBinding
    @Interceptor
    static class SimpleInterceptor {

        @Inject
        Charlie charlie;

        @AroundInvoke
        Object aroundInvoke(InvocationContext context) throws Exception {
            return Boolean.parseBoolean(context.proceed().toString()) ? charlie.ping() : "false";
        }

    }

}
