package io.quarkus.arc.test.interceptors;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class KotlinInterceptorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToUpperCaseConverter.class,
            FailingInterceptor.class, AlwaysFail.class);

    @Test
    public void testInterceptionThrowsUnwrapped() {
        ToUpperCaseConverter converter = Arc.container().instance(ToUpperCaseConverter.class).get();
        assertThrows(IOException.class, () -> converter.convert("holA!"));
    }

    interface Converter<T> {

        T convert(T value);

    }

    @AlwaysFail
    @ApplicationScoped
    @kotlin.Metadata
    static class ToUpperCaseConverter implements Converter<String> {

        @Override
        public String convert(String value) {
            return value.toUpperCase();
        }
    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface AlwaysFail {
    }

    @AlwaysFail
    @Priority(1)
    @Interceptor
    static class FailingInterceptor {

        @AroundInvoke
        Object fail(InvocationContext ctx) throws Exception {
            throw new IOException();
        }

    }

}
