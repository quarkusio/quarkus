package io.quarkus.arc.test.validation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;

public class NormalScopedConstructorTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Unproxyable.class).shouldFail().build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DeploymentException);
        assertNotNull(error.getCause());
        assertTrue(error.getCause() instanceof DefinitionException);
    }

    @ApplicationScoped
    static class Unproxyable {

        @Inject
        public Unproxyable(Instance<String> instance) {
        }

    }

}
