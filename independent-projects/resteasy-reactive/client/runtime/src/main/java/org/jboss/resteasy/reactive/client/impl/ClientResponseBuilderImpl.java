package org.jboss.resteasy.reactive.client.impl;

import java.io.InputStream;
import java.net.URI;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.common.NotImplementedYet;
import org.jboss.resteasy.reactive.common.jaxrs.ResponseBuilderImpl;
import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;

public class ClientResponseBuilderImpl extends ResponseBuilderImpl { //TODO: should not extend the server version

    InputStream entityStream;
    RestClientRequestContext restClientRequestContext;

    public ClientResponseBuilderImpl invocationState(RestClientRequestContext restClientRequestContext) {
        this.restClientRequestContext = restClientRequestContext;
        return this;
    }

    public ClientResponseBuilderImpl entityStream(InputStream entityStream) {
        this.entityStream = entityStream;
        return this;
    }

    @Override
    protected ResponseBuilderImpl doClone() {
        return new ClientResponseBuilderImpl();
    }

    @Override
    public ResponseImpl build() {
        ClientResponseImpl response = new ClientResponseImpl();
        populateResponse(response);
        response.restClientRequestContext = restClientRequestContext;
        response.setEntityStream(entityStream);
        return response;
    }

    @Override
    public Response.ResponseBuilder contentLocation(URI location) {
        //TODO: needs some thinking
        throw new NotImplementedYet();
    }

    @Override
    public Response.ResponseBuilder location(URI location) {
        throw new NotImplementedYet();
    }
}
