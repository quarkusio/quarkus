package io.quarkus.arc.test.interceptors.aroundconstruct;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.TransientReference;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class AroundConstructWithTransientReferencesTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding.class, MyInterceptor.class, MyBean.class,
            MyDependency.class);

    @Test
    public void test() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertNotNull(bean);
        assertTrue(MyBean.constructed);
        assertTrue(MyInterceptor.intercepted);
        assertTrue(MyDependency.constructed);
        assertTrue(MyDependency.destroyed);
    }

    @Target({ TYPE, CONSTRUCTOR })
    @Retention(RUNTIME)
    @InterceptorBinding
    public @interface MyBinding {
    }

    @MyBinding
    @Interceptor
    @Priority(1)
    public static class MyInterceptor {
        static boolean intercepted = false;

        @AroundConstruct
        void intercept(InvocationContext ctx) throws Exception {
            intercepted = true;
            ctx.proceed();
        }
    }

    @Dependent
    static class MyBean {
        static boolean constructed = false;

        @Inject
        @MyBinding
        MyBean(@TransientReference MyDependency dependency) {
            constructed = true;
        }
    }

    @Dependent
    static class MyDependency {
        static boolean constructed = false;
        static boolean destroyed = false;

        @PostConstruct
        public void construct() {
            constructed = true;
        }

        @PreDestroy
        public void destroy() {
            destroyed = true;
        }
    }
}
