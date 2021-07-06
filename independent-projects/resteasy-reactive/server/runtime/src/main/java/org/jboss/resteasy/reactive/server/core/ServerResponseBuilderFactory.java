package org.jboss.resteasy.reactive.server.core;

import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.core.ResponseBuilderFactory;
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
}
