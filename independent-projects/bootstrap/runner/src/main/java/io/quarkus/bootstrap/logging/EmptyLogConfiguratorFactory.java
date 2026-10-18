package io.quarkus.bootstrap.logging;

import java.io.InputStream;

import org.jboss.logmanager.ConfiguratorFactory;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.LogContextConfigurator;

public class EmptyLogConfiguratorFactory implements ConfiguratorFactory {

    @Override
    public LogContextConfigurator create() {
        return new LogContextConfigurator() {
            @Override
            public void configure(LogContext logContext, InputStream inputStream) {
                // do nothing
            }
        };
    }

    @Override
    public int priority() {
        // Lower than org.jboss.logmanager.configuration.DefaultConfiguratorFactory
        // We don't use Integer.MIN_VALUE to allow this to be overridden if necessary
        return 50;
    }
}
