package org.jboss.resteasy.reactive.server.core;

import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.common.core.ResponseBuilderFactory;
import org.jboss.resteasy.reactive.server.jaxrs.ResponseBuilderImpl;

public class ServerResponseBuilderFactory implements ResponseBuilderFactory {
    @Override
    public Response.ResponseBuilder create() {
        return new ResponseBuilderImpl();
    }

    @Override
    public int priority() {
        return 100;
    }
}
