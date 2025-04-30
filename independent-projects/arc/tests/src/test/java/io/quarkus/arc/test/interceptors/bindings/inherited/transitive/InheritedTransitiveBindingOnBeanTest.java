package io.quarkus.arc.test.interceptors.bindings.inherited.transitive;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
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

public class InheritedTransitiveBindingOnBeanTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer.Builder()
            .beanClasses(MyBean.class,
                    FooBinding.class, BarBinding.class, BazBinding.class,
                    FooInterceptor.class, BarInterceptor.class, BazInterceptor.class)
            .build();

    @Test
    public void testInterception() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertNotNull(bean);
        bean.doSomething();
        assertTrue(FooInterceptor.intercepted);
        assertFalse(BarInterceptor.intercepted);
        assertTrue(BazInterceptor.intercepted);
    }

    @FooBinding
    @BarBinding
    static class MySuperclass {
    }

    @Singleton
    static class MyBean extends MySuperclass {
        void doSomething() {
        }
    }

    @BazBinding
    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    @Inherited
    // must be `public`, otherwise the `_ComponentsProvider` would fail verification, because
    // it accesses the `FooBinding.class` directly (we should perhaps change that to `String`?)
    public @interface FooBinding {
    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    // not @Inherited
    @interface BarBinding {
    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    // not @Inherited
    @interface BazBinding {
    }

    @FooBinding
    @Interceptor
    @Priority(1)
    static class FooInterceptor {
        static boolean intercepted = false;

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            intercepted = true;
            return ctx.proceed();
        }
    }

    @BarBinding
    @Interceptor
    @Priority(1)
    static class BarInterceptor {
        static boolean intercepted = false;

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            intercepted = true;
            return ctx.proceed();
        }
    }

    @BazBinding
    @Interceptor
    @Priority(1)
    static class BazInterceptor {
        static boolean intercepted = false;

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            intercepted = true;
            return ctx.proceed();
        }
    }
}
