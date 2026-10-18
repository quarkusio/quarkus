package io.quarkus.arc.test.decorators.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.decorators.interceptor.other.Converter;
import io.quarkus.arc.test.decorators.interceptor.other.Logging;
import io.quarkus.arc.test.decorators.interceptor.other.ToUpperCaseConverter;

public class InterceptorAndNonPublicDecoratorTest {

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
