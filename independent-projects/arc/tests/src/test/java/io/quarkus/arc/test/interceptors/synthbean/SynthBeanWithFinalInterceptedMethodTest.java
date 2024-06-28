package io.quarkus.arc.test.interceptors.synthbean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SynthBeanWithFinalInterceptedMethodTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyBinding.class, MyInterceptor.class)
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure(MyNonbean.class)
                            .types(MyNonbean.class)
                            .injectInterceptionProxy(MyNonbean.class)
                            .creator(MyNonbeanCreator.class)
                            .done();
                }
            })
            .shouldFail()
            .build();

    static class MyNonbeanCreator implements BeanCreator<MyNonbean> {
        @Override
        public MyNonbean create(SyntheticCreationalContext<MyNonbean> context) {
            InterceptionProxy<MyNonbean> proxy = context.getInterceptionProxy();
            return proxy.create(new MyNonbean());
        }
    }

    @Test
    public void test() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DeploymentException);
        assertTrue(error.getMessage().contains("may not be declared final"));
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
        final String hello() {
            return "hello";
        }
    }
}
