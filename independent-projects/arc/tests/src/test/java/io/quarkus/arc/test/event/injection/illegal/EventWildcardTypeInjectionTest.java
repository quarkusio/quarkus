package io.quarkus.arc.test.event.injection.illegal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.test.ArcTestContainer;

public class EventWildcardTypeInjectionTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(WrongBean.class).shouldFail()
            .build();

    @Test
    public void testExceptionIsThrown() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage()
                .contains("Wildcard without lower bound is not a legal type argument for jakarta.enterprise.event.Event"));
    }

    @Singleton
    @Unremovable
    static class WrongBean {

        // wildcard event type
        @Inject
        Event<? extends Integer> event;

    }
}
