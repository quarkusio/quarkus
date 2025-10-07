package io.quarkus.arc.test.event.injection.illegal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class InitMethodEventWildcardTypeTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(InvalidBean.class).shouldFail()
            .build();

    @Test
    public void testExceptionIsThrown() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage()
                .contains("Wildcard without lower bound is not a legal type argument for jakarta.enterprise.event.Event"));
    }

    @Dependent
    public static class InvalidBean {

        // wildcard event type
        @Inject
        public void initMethod(Event<?> event) {
        }

    }
}
