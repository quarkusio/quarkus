package io.quarkus.arc.test.event.injection.invalid;

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

public class EventRawTypeInjectionTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(WrongBean.class).shouldFail()
            .build();

    @Test
    public void testExceptionIsThrown() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
    }

    @Singleton
    @Unremovable
    static class WrongBean {

        @Inject
        Event rawEvent;

    }
}
