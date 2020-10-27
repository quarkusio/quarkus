package io.quarkus.rest.server.runtime.spi;

import javax.ws.rs.container.ContainerRequestContext;

import io.quarkus.rest.common.runtime.core.QuarkusRestContext;

public interface QuarkusRestContainerRequestContext extends ContainerRequestContext {

    public QuarkusRestContext getQuarkusRestContext();

    public void suspend();

    public void resume();

    public void resume(Throwable t);

}
