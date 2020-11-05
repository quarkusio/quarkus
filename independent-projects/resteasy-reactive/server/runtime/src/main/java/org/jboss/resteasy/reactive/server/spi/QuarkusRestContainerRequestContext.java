package org.jboss.resteasy.reactive.server.spi;

import javax.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.common.runtime.core.QuarkusRestContext;

public interface QuarkusRestContainerRequestContext extends ContainerRequestContext {

    public QuarkusRestContext getQuarkusRestContext();

    public void suspend();

    public void resume();

    public void resume(Throwable t);

}
