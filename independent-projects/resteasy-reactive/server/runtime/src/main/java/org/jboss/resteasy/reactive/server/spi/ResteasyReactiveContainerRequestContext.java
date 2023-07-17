package org.jboss.resteasy.reactive.server.spi;

import jakarta.ws.rs.container.ContainerRequestContext;

public interface ResteasyReactiveContainerRequestContext extends ContainerRequestContext {

    ServerRequestContext getServerRequestContext();

    void suspend();

    void resume();

    void resume(Throwable t);

}
