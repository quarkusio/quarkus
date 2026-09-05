package org.jboss.resteasy.reactive.server.core;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.core.ResponseBuilderFactory;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.server.jaxrs.ResponseBuilderImpl;
import org.jboss.resteasy.reactive.server.jaxrs.RestResponseBuilderImpl;

public class ServerResponseBuilderFactory implements ResponseBuilderFactory {
    @Override
    public Response.ResponseBuilder create() {
        return new ResponseBuilderImpl();
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public <T> RestResponse.ResponseBuilder<T> createRestResponse() {
        return new RestResponseBuilderImpl();
    }

    @Override
    public Serialisers getSerialisers() {
        try {
            ResteasyReactiveRequestContext ctx = CurrentRequestManager.get();
            return ctx != null ? ctx.getDeployment().getSerialisers() : null;
        } catch (ContextNotActiveException e) {
            return null;
        }
    }
}
