package io.quarkus.arc.test.interceptors.aroundconstruct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class AroundConstructExceptionHandlingTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(SimpleBean.class,
            MyInterceptorBinding.class, MyInterceptor1.class, MyInterceptor2.class);

    @Test
    public void test() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            Arc.container().instance(SimpleBean.class).get();
        });

        assertTrue(MyInterceptor1.intercepted);
        assertTrue(MyInterceptor2.intercepted);

        assertNotNull(e.getCause());
        assertEquals(IllegalArgumentException.class, e.getCause().getClass());
        assertEquals("intentional", e.getCause().getMessage());
    }

    @Singleton
    @MyInterceptorBinding
    static class SimpleBean {
    }

    @Target({ ElementType.TYPE, ElementType.CONSTRUCTOR })
    @Retention(RetentionPolicy.RUNTIME)
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor1 {
        static boolean intercepted = false;

        @AroundConstruct
        void aroundConstruct(InvocationContext ctx) throws Exception {
            intercepted = true;
            try {
                ctx.proceed();
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(2)
    static class MyInterceptor2 {
        static boolean intercepted = false;

        @AroundConstruct
        void aroundConstruct(InvocationContext ctx) {
            intercepted = true;
            throw new IllegalArgumentException("intentional");
        }
    }
}
