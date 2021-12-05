package io.quarkus.arc.test.interceptor;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.DefinitionException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InterceptorNoBindingsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(InterceptorWithoutBindings.class))
            .setExpectedException(DefinitionException.class);

    @Test
    public void testDeploymentFailed() {
        // This method should not be invoked
    }

    @Priority(1)
    @Interceptor
    static class InterceptorWithoutBindings {

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }

    }

}
