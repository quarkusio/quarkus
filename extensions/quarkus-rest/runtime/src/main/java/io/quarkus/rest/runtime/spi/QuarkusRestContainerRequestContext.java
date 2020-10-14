package io.quarkus.rest.runtime.spi;

import javax.ws.rs.container.ContainerRequestContext;

public interface QuarkusRestContainerRequestContext extends ContainerRequestContext {

    public QuarkusRestContext getQuarkusRestContext();

    public void suspend();

    public void resume();

    public void resume(Throwable t);

}
