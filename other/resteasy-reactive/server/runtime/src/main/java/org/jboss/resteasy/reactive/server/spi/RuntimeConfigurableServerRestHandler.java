package org.jboss.resteasy.reactive.server.spi;

public interface RuntimeConfigurableServerRestHandler extends ServerRestHandler {

    void configure(RuntimeConfiguration configuration);
}
