package io.quarkus.arc.test.validation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import org.junit.Rule;
import org.junit.Test;

public class BoundInterceptorPrivateNoArgsConstructorTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Unproxyable.class, Simple.class, SimpleInterceptor.class).shouldFail().build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DeploymentException);
        assertNotNull(error.getCause());
        assertTrue(error.getCause() instanceof DefinitionException);
    }

    @Dependent
    @Simple
    static class Unproxyable {

        private Unproxyable() {
        }

        void ping() {
        }

    }

}
