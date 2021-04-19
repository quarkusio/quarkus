package org.jboss.resteasy.reactive.client.spi;

import javax.ws.rs.client.ClientRequestContext;

public interface ResteasyReactiveClientRequestContext extends ClientRequestContext {

    void suspend();

    void resume();

    void resume(Throwable t);
}
