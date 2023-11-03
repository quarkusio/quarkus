package io.quarkus.arc.test.interceptors.initializer;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
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
import io.quarkus.arc.test.interceptors.initializer.subpkg.MyDependency;
import io.quarkus.arc.test.interceptors.initializer.subpkg.MySuperclass;

public class InitializerMethodInterceptionTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyDependency.class, MyBean.class, MyInterceptorBinding.class,
            MyInterceptor.class);

    @Test
    public void testInterception() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertNotNull(bean);
        assertTrue(bean.publicInitializerCalled);
        assertTrue(bean.protectedInitializerCalled);
        assertTrue(bean.packagePrivateInitializerCalled);
        assertTrue(bean.privateInitializerCalled);

        // initializer methods invoked by the container are not intercepted
        assertFalse(MyInterceptor.intercepted);
    }

    // note that `MySuperclass` is intentionally in a different package
    @Singleton
    @MyInterceptorBinding
    static class MyBean extends MySuperclass {
    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor {
        static boolean intercepted = false;

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            intercepted = true;
            return ctx.proceed();
        }
    }
}
