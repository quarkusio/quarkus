package org.jboss.resteasy.reactive.server.spi;

@Deprecated
public interface RuntimeConfigurableServerRestHandler
        extends GenericRuntimeConfigurableServerRestHandler<RuntimeConfiguration> {

    @Override
    default Class<RuntimeConfiguration> getConfigurationClass() {
        return RuntimeConfiguration.class;
    }

    void configure(RuntimeConfiguration configuration);
}
