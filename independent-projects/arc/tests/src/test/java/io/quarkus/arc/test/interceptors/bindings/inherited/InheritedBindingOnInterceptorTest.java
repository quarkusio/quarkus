package io.quarkus.arc.test.interceptors.bindings.inherited;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class InheritedBindingOnInterceptorTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, FooBinding.class, BarBinding.class,
            BarBindingUnused.class, FooInterceptor.class, BarInterceptor.class);

    @Test
    public void testInterception() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertNotNull(bean);
        bean.doSomething();
        assertTrue(FooInterceptor.intercepted);
        assertFalse(BarInterceptor.intercepted);
    }

    @Singleton
    @FooBinding
    @BarBinding
    static class MyBean {
        void doSomething() {
        }
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    @Inherited
    @interface FooBinding {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    // not @Inherited
    @interface BarBinding {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    @interface BarBindingUnused {
    }

    @FooBinding
    static class FooInterceptorSuperclass {
    }

    @Interceptor
    @Priority(1)
    static class FooInterceptor extends FooInterceptorSuperclass {
        static boolean intercepted = false;

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            intercepted = true;
            return ctx.proceed();
        }
    }

    @BarBinding
    static class BarInterceptorSuperclass {
    }

    @BarBindingUnused // just to prevent the "Interceptor has no bindings" error, not used otherwise
    @Interceptor
    @Priority(1)
    static class BarInterceptor extends BarInterceptorSuperclass {
        static boolean intercepted = false;

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            intercepted = true;
            return ctx.proceed();
        }
    }
}
