package io.quarkus.arc.test.records;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class InterceptedRecordTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(DependentRecord.class, MyInterceptorBinding.class, MyInterceptor.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DeploymentException.class, error);
        assertTrue(error.getMessage().contains("records are always final"));
    }

    @Dependent
    record DependentRecord() {
        @MyInterceptorBinding
        String hello() {
            return "hello";
        }
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "intercepted: " + ctx.proceed();
        }
    }
}
