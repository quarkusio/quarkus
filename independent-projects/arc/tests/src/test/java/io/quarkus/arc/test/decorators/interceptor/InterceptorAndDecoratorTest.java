package io.quarkus.arc.test.decorators.interceptor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InterceptorAndDecoratorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToUpperCaseConverter.class,
            TrimConverterDecorator.class, LoggingInterceptor.class, Logging.class);

    @Test
    public void testInterceptionAndDecoration() {
        LoggingInterceptor.LOG.set(null);
        ToUpperCaseConverter converter = Arc.container().instance(ToUpperCaseConverter.class).get();
        assertEquals("HOLA!", converter.convert(" holA!"));
        assertEquals("HOLA!", LoggingInterceptor.LOG.get());
        assertEquals(" HOLA!", converter.convertNoDelegation(" holA!"));
        assertEquals(" HOLA!", LoggingInterceptor.LOG.get());
    }

    interface Converter<T> {

        T convert(T value);

    }

    @Logging
    @ApplicationScoped
    static class ToUpperCaseConverter implements Converter<String> {

        @Override
        public String convert(String value) {
            return value.toUpperCase();
        }

        public String convertNoDelegation(String value) {
            return value.toUpperCase();
        }

    }

    @Priority(1)
    @Decorator
    static class TrimConverterDecorator implements Converter<String> {

        @Inject
        @Delegate
        Converter<String> delegate;

        @Override
        public String convert(String value) {
            return delegate.convert(value.trim());
        }

    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface Logging {

    }

    @Logging
    @Priority(10)
    @Interceptor
    static class LoggingInterceptor {

        static final AtomicReference<Object> LOG = new AtomicReference<Object>();

        @AroundInvoke
        Object log(InvocationContext ctx) throws Exception {
            Object ret = ctx.proceed();
            LOG.set(ret);
            return ret;
        }
    }

}
