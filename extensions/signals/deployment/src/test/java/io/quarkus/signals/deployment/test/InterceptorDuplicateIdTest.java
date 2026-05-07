package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.spi.ReceiverInterceptor;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

public class InterceptorDuplicateIdTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(FooInterceptor.class, BarInterceptor.class))
            .setExpectedException(IllegalStateException.class, true);

    @Test
    public void testFailure() {
        fail();
    }

    @Identifier("foo")
    @Singleton
    public static class FooInterceptor implements ReceiverInterceptor {

        @Override
        public Uni<Object> intercept(InterceptionContext context) {
            return context.proceed();
        }
    }

    @Identifier("foo")
    @Singleton
    public static class BarInterceptor implements ReceiverInterceptor {

        @Override
        public Uni<Object> intercept(InterceptionContext context) {
            return context.proceed();
        }
    }
}
