package io.quarkus.arc.test.instance.illegal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class InstanceOfWildcardWithUpperBoundOfEventOfWildcardWithUpperBoundInjectionPointTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Head.class)
            .shouldFail()
            .build();;

    @Test
    public void testError() {
        Throwable failure = container.getFailure();
        assertNotNull(failure);
        assertInstanceOf(DefinitionException.class, failure);
        assertTrue(failure.getMessage().contains(
                "Wildcard jakarta.enterprise.event.Event without lower bound is not a legal type argument for jakarta.enterprise.inject.Instance"));
    }

    @Dependent
    static class Head {
        @Inject
        Instance<? extends Event<? extends Number>> instance;
    }
}
