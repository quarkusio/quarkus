package io.quarkus.arc.test.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DefinitionException;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InitializerMethodMarkedProducerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Producers.class).shouldFail().build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
    }

    @ApplicationScoped
    static class Producers {
        @Inject
        @Produces
        @ApplicationScoped
        MyBean produce() {
            return new MyBean();
        }
    }

    static class MyBean {
    }
}
