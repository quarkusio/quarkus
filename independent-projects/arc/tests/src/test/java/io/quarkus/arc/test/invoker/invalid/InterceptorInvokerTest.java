package io.quarkus.arc.test.invoker.invalid;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InterceptorInfo;
import io.quarkus.arc.test.ArcTestContainer;

public class InterceptorInvokerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyInterceptor.class, MyInterceptorBinding.class)
            .beanRegistrars(ctx -> {
                InterceptorInfo bean = ctx.get(BuildExtension.Key.INTERCEPTORS)
                        .stream()
                        .filter(it -> it.getBeanClass().equals(DotName.createSimple(MyInterceptor.class)))
                        .findAny()
                        .orElseThrow();
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                ctx.getInvokerFactory().createInvoker(bean, method);
            })
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DeploymentException.class, error);
        assertTrue(error.getMessage().contains("Cannot build invoker for target bean"));
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @InterceptorBinding
    public @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor {
        String hello() {
            return "foobar";
        }

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }
    }
}
