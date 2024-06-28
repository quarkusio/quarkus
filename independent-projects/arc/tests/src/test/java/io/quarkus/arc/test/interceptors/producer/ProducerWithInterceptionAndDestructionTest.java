package io.quarkus.arc.test.interceptors.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.test.ArcTestContainer;

public class ProducerWithInterceptionAndDestructionTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyDependency.class, MyBinding.class, MyInterceptor.class,
            MyProducer.class);

    @Test
    public void test() {
        InstanceHandle<MyNonbean> instance = Arc.container().instance(MyNonbean.class);
        MyNonbean nonbean = instance.get();
        assertEquals("intercepted: hello", nonbean.hello());

        assertFalse(MyProducer.disposed);
        assertFalse(MyDependency.destroyed);
        instance.destroy();
        assertTrue(MyProducer.disposed);
        // this seems to be underspecified and when using `InterceptionFactory`, it is `false` in Weld too
        // (technically, it could be `true` as well, but I didn't look too deep into what that would take)
        assertFalse(MyDependency.destroyed);
    }

    @Dependent
    static class MyDependency {
        static boolean destroyed = false;

        @PreDestroy
        void destroy() {
            destroyed = true;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
    @InterceptorBinding
    @interface MyBinding {
    }

    @MyBinding
    @Priority(1)
    @Interceptor
    static class MyInterceptor {
        @Inject
        MyDependency dependency;

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "intercepted: " + ctx.proceed();
        }
    }

    static class MyNonbean {
        @MyBinding
        String hello() {
            return "hello";
        }
    }

    @Dependent
    static class MyProducer {
        static boolean disposed = false;

        @Produces
        MyNonbean produce(InterceptionProxy<MyNonbean> proxy) {
            return proxy.create(new MyNonbean());
        }

        void dispose(@Disposes MyNonbean nonbean) {
            disposed = true;
        }
    }
}
