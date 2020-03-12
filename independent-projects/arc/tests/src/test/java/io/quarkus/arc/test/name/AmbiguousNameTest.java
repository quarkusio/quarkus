package io.quarkus.arc.test.name;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Named;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class AmbiguousNameTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Bravo.class, Alpha.class)
            .shouldFail()
            .build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DeploymentException);
    }

    @Named("A")
    @Singleton
    static class Alpha {
    }

    @Named("A")
    @Dependent
    static class Bravo {
    }

}
