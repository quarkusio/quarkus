package io.quarkus.arquillian;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

/**
 * Activates request context before test runs and shuts it down afterwards
 */
public class RequestContextLifecycle {
    public void on(@Observes(precedence = -100) Before event) throws Throwable {
        ArcContainer container = Arc.container();
        if (container != null && container.isRunning()) {
            container.requestContext().activate();
        }
    }

    public void on(@Observes(precedence = 100) After event) throws Throwable {
        ArcContainer container = Arc.container();
        if (container != null && container.isRunning()) {
            container.requestContext().terminate();
        }
    }
}
