package io.quarkus.bootstrap.logging;

import java.io.InputStream;

import org.jboss.logmanager.ConfiguratorFactory;
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

    public static final class Factory implements ConfiguratorFactory {

        @Override
        public LogContextConfigurator create() {
            return new EmptyLogContextConfigurator();
        }

        @Override
        public int priority() {
            // Lower than org.jboss.logmanager.configuration.DefaultConfiguratorFactory
            // We don't use Integer.MIN_VALUE to allow this to be overridden if necessary
            return 50;
        }
    }
}
