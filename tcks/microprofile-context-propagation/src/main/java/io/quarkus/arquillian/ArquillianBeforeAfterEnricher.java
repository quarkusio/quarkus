package io.quarkus.arquillian;

import org.jboss.arquillian.core.api.annotation.Observes;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

/**
 * Activates request context before test runs and shuts it down afterwards
 */
public class ArquillianBeforeAfterEnricher {

    private static final String ERROR_MSG = "Arc container is not running, cannot activate CDI contexts!";

    public void on(@Observes(precedence = -100) org.jboss.arquillian.test.spi.event.suite.Before event) throws Throwable {
        ArcContainer container = Arc.container();
        if (container.isRunning()) {
            container.requestContext().activate();
        } else {
            throw new IllegalStateException(ERROR_MSG);
        }
    }

    public void on(@Observes(precedence = 100) org.jboss.arquillian.test.spi.event.suite.After event) throws Throwable {
        ArcContainer container = Arc.container();
        if (container.isRunning()) {
            container.requestContext().terminate();
        } else {
            throw new IllegalStateException(ERROR_MSG);
        }
    }
}
