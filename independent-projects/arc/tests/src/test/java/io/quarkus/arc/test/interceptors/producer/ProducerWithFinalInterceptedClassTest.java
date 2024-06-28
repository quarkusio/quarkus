package io.quarkus.arc.test.interceptors.producer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.test.ArcTestContainer;

public class ProducerWithFinalInterceptedClassTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyBinding.class, MyInterceptor.class, MyProducer.class)
            .shouldFail()
            .build();

    @Test
    public void test() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DeploymentException);
        assertTrue(error.getMessage().contains("must not be final"));
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

    static final class MyNonbean {
        @MyBinding
        String hello() {
            return "hello";
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
