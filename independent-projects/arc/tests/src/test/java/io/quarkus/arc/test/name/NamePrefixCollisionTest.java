package io.quarkus.arc.test.name;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class NamePrefixCollisionTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Alpha.class, Bravo.class)
            .strictCompatibility(true)
            .shouldFail()
            .build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DeploymentException);
        assertTrue(error.getMessage().contains("identical to a bean name prefix used elsewhere"));
    }

    @Named("x")
    @Dependent
    static class Alpha {
    }

    @Named("x.y")
    @Dependent
    static class Bravo {
    }
}
