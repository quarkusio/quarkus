package io.quarkus.arc.test.decorators.abstractimpl;

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

public class InterceptorAndAbstractDecoratorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToUpperCaseConverter.class,
            TrimConverterDecorator.class, LoggingInterceptor.class, Logging.class);

    @Test
    public void testInterceptionAndDecoration() {
        LoggingInterceptor.LOG.set(null);
        ToUpperCaseConverter converter = Arc.container().instance(ToUpperCaseConverter.class).get();
        assertEquals("HELLO", converter.convert(" hello "));
        assertEquals("HELLO", LoggingInterceptor.LOG.get());
        assertEquals(ToUpperCaseConverter.class.getName(), converter.getId());
        assertEquals(ToUpperCaseConverter.class.getName(), LoggingInterceptor.LOG.get());
    }

    interface Converter<T, U> {

        T convert(T value);

        U getId();

    }

    @Logging
    @ApplicationScoped
    static class ToUpperCaseConverter implements Converter<String, String> {

        @Override
        public String convert(String value) {
            return value.toUpperCase();
        }

        @Override
        public String getId() {
            return ToUpperCaseConverter.class.getName();
        }

    }

    @Priority(1)
    @Decorator
    static abstract class TrimConverterDecorator implements Converter<String, String> {

        @Inject
        @Delegate
        Converter<String, String> delegate;

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
