package org.jboss.resteasy.reactive.server.spi;

import jakarta.ws.rs.core.Response;

public interface AsyncExceptionMapperContext {

    ServerRequestContext serverRequestContext();

    void suspend();

    void resume();

    void setResponse(Response response);
}
