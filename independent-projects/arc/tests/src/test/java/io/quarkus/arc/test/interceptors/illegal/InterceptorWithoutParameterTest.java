package io.quarkus.arc.test.interceptors.illegal;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class InterceptorWithoutParameterTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyInterceptor.class, MyInterceptorBinding.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains(
                "@AroundInvoke interceptor method declared in an interceptor class must have exactly one parameter"));
        assertTrue(error.getMessage().contains("intercept()"));
        assertTrue(error.getMessage().contains("InterceptorWithoutParameterTest$MyInterceptor"));
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor {
        @AroundInvoke
        Object intercept() throws Exception {
            return null;
        }
    }
}
