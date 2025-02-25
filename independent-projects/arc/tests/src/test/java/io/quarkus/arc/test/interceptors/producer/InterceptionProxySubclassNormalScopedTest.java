package io.quarkus.arc.test.interceptors.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.InterceptionProxySubclass;
import io.quarkus.arc.test.ArcTestContainer;

public class InterceptionProxySubclassNormalScopedTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding.class, MyInterceptor.class, MyProducer.class);

    @Test
    public void test() {
        MyNonbean nonbean = Arc.container().instance(MyNonbean.class).get();
        assertEquals("intercepted: hello", nonbean.hello());

        assertInstanceOf(ClientProxy.class, nonbean);
        assertNotNull(ClientProxy.unwrap(nonbean));
        assertNotSame(nonbean, ClientProxy.unwrap(nonbean));

        MyNonbean unwrapped = ClientProxy.unwrap(nonbean);

        assertInstanceOf(InterceptionProxySubclass.class, unwrapped);
        assertNotNull(InterceptionProxySubclass.unwrap(unwrapped));
        assertNotSame(unwrapped, InterceptionProxySubclass.unwrap(unwrapped));
        assertNotSame(nonbean, InterceptionProxySubclass.unwrap(unwrapped));
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
        @Produces
        @ApplicationScoped
        MyNonbean produce(InterceptionProxy<MyNonbean> proxy) {
            return proxy.create(new MyNonbean());
        }
    }
}
