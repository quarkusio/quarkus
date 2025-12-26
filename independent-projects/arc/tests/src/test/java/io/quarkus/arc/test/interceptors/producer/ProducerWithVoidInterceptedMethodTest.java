package io.quarkus.arc.test.interceptors.producer;

import static io.smallrye.common.constraint.Assert.assertFalse;
import static io.smallrye.common.constraint.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.test.ArcTestContainer;

public class ProducerWithVoidInterceptedMethodTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding.class, MyInterceptor.class, MyProducer.class);

    @Test
    public void test() {
        assertFalse(MyNonbean.invoked);
        assertFalse(MyInterceptor.invoked);
        MyNonbean nonbean = Arc.container().instance(MyNonbean.class).get();
        nonbean.hello();
        assertTrue(MyNonbean.invoked);
        assertTrue(MyInterceptor.invoked);
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
        static boolean invoked = false;

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            invoked = true;
            return ctx.proceed();
        }
    }

    static class MyNonbean {
        static boolean invoked = false;

        @MyBinding
        void hello() {
            invoked = true;
        }
    }

    @Dependent
    static class MyProducer {
        @Produces
        MyNonbean produce(InterceptionProxy<MyNonbean> proxy) {
            return proxy.create(new MyNonbean());
        }
    }
}
