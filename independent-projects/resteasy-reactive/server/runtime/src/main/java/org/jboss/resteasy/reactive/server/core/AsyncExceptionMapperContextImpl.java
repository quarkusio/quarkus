package org.jboss.resteasy.reactive.server.core;

import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.spi.AsyncExceptionMapperContext;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class AsyncExceptionMapperContextImpl implements AsyncExceptionMapperContext {

    private final ResteasyReactiveRequestContext context;

    public AsyncExceptionMapperContextImpl(ResteasyReactiveRequestContext context) {
        this.context = context;
    }

    @Override
    public ServerRequestContext serverRequestContext() {
        return context;
    }

    @Override
    public void suspend() {
        context.suspend();
    }

    @Override
    public void resume() {
        context.resume();
    }

    @Override
    public void setResponse(Response response) {
        context.setResult(response);
    }
}
