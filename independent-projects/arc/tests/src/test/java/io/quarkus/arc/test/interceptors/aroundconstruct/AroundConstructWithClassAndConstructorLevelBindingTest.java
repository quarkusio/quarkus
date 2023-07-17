package io.quarkus.arc.test.interceptors.aroundconstruct;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class AroundConstructWithClassAndConstructorLevelBindingTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding.class, MyInterceptor1.class,
            MyInterceptor2.class, MyBean.class);

    @Test
    public void test() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertNotNull(bean);
        assertTrue(MyBean.constructed);
        assertFalse(MyInterceptor1.intercepted);
        assertTrue(MyInterceptor2.intercepted);
    }

    @Target({ TYPE, CONSTRUCTOR })
    @Retention(RUNTIME)
    @InterceptorBinding
    public @interface MyBinding {
        int value();
    }

    @MyBinding(1)
    @Interceptor
    @Priority(1)
    public static class MyInterceptor1 {
        static boolean intercepted = false;

        @AroundConstruct
        void intercept(InvocationContext ctx) throws Exception {
            intercepted = true;
            ctx.proceed();
        }
    }

    @MyBinding(2)
    @Interceptor
    @Priority(1)
    public static class MyInterceptor2 {
        static boolean intercepted = false;

        @AroundConstruct
        void intercept(InvocationContext ctx) throws Exception {
            intercepted = true;
            ctx.proceed();
        }
    }

    @Dependent
    @MyBinding(1)
    static class MyBean {
        static boolean constructed = false;

        @Inject
        @MyBinding(2)
        MyBean() {
            constructed = true;
        }
    }
}
