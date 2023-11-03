package io.quarkus.arc.test.interceptors.inheritance.hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class MultipleInterceptorMethodDeclaredOnSuperclassTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(AlphaInterceptor.class, Bravo.class, AlphaBinding.class,
                    Fool.class)
            .shouldFail().build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertEquals(
                "Multiple @AroundInvoke interceptor methods declared on class: io.quarkus.arc.test.interceptors.inheritance.hierarchy.MultipleInterceptorMethodDeclaredOnSuperclassTest$Bravo",
                error.getMessage());
    }

    @Priority(1)
    @AlphaBinding
    @Interceptor
    static class AlphaInterceptor extends Bravo {

        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            return "a/" + ctx.proceed() + "/a";
        }

    }

    static class Bravo {

        @AroundInvoke
        public Object b1(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }

        @AroundInvoke
        public Object b2(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }
    }

    @AlphaBinding
    @ApplicationScoped
    public static class Fool {

        String ping() {
            return "ping";
        }

    }

}
