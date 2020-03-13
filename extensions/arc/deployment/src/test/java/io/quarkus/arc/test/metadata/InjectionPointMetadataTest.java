package io.quarkus.arc.test.metadata;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InjectionPointMetadataTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(InvalidInjection.class))
            .setExpectedException(DefinitionException.class);

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        Assertions.fail();
    }

    @ApplicationScoped
    static class InvalidInjection {

        @Inject
        InjectionPoint injectionPoint;

    }

}
