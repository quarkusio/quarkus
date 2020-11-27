package org.jboss.resteasy.reactive.server.spi;

import javax.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.common.core.ResteasyReactiveCallbackContext;

public interface ResteasyReactiveContainerRequestContext extends ContainerRequestContext {

    ResteasyReactiveCallbackContext getQuarkusRestContext();

    void suspend();

    void resume();

    void resume(Throwable t);

}
