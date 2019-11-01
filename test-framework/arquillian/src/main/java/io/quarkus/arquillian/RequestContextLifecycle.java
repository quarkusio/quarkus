package io.quarkus.arquillian;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

/**
 * Activates request context before test runs and shuts it down afterwards
 */
public class RequestContextLifecycle {

    private static final Logger LOGGER = Logger.getLogger(RequestContextLifecycle.class);

    private static final int DEFAULT_PRECEDENCE = 100;

    public void on(@Observes(precedence = DEFAULT_PRECEDENCE) Before event) throws Throwable {
        ArcContainer container = Arc.container();
        if (container != null && container.isRunning()) {
            container.requestContext().activate();
            LOGGER.debug("RequestContextLifecycle activating CDI Request context.");
        }
    }

    public void on(@Observes(precedence = DEFAULT_PRECEDENCE) After event) throws Throwable {
        ArcContainer container = Arc.container();
        if (container != null && container.isRunning()) {
            container.requestContext().terminate();
            LOGGER.debug("RequestContextLifecycle shutting down CDI Request context.");
        }
    }
}
