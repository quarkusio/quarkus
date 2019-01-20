package org.jboss.protean.arc.test.validation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class NormalScopedFinalTest {

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
    static final class Unproxyable {

        void ping() {
        }
        
    }

}
