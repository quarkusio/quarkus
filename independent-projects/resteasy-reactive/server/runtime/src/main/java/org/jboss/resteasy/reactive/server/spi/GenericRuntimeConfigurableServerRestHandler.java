package org.jboss.resteasy.reactive.server.spi;

public interface GenericRuntimeConfigurableServerRestHandler<T> extends ServerRestHandler {

    Class<T> getConfigurationClass();

    void configure(T configuration);
}
