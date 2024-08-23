package io.quarkus.bootstrap.logging;

import java.io.InputStream;

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.LogContextConfigurator;

/**
 * An empty log context configuration which prevents the logmanager from configuring itself too early.
 */
public final class EmptyLogContextConfigurator implements LogContextConfigurator {
    public EmptyLogContextConfigurator() {
    }

    public void configure(final LogContext logContext, final InputStream inputStream) {
        // no operation
    }
}
