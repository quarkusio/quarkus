package io.quarkus.arc.test.producer.illegal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import java.time.temporal.Temporal;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DefinitionException;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ProducerFieldWithInjectTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(IllegalProducer.class).shouldFail()
            .build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
    }

    @Dependent
    static class IllegalProducer {

        @Inject
        @Produces
        Temporal temporal;

    }

}
