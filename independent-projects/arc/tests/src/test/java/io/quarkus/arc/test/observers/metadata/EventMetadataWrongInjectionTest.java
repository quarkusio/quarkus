package io.quarkus.arc.test.observers.metadata;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.EventMetadata;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class EventMetadataWrongInjectionTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(WrongBean.class).shouldFail().build();

    @Test
    public void testMetadata() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DeploymentException);
        assertNotNull(error.getCause());
        assertTrue(error.getCause() instanceof DefinitionException);
    }

    @Singleton
    static class WrongBean {

        @Inject
        EventMetadata metadata;

    }

}
