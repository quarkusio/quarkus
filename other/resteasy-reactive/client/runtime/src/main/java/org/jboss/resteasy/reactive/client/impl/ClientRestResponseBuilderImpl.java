package org.jboss.resteasy.reactive.client.impl;

import java.io.InputStream;
import java.net.URI;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.NotImplementedYet;
import org.jboss.resteasy.reactive.common.jaxrs.AbstractRestResponseBuilder;
import org.jboss.resteasy.reactive.common.jaxrs.RestResponseImpl;

public class ClientRestResponseBuilderImpl<T> extends AbstractRestResponseBuilder<T> { //TODO: should not extend the server version

    InputStream entityStream;
    RestClientRequestContext restClientRequestContext;

    public ClientRestResponseBuilderImpl<T> invocationState(RestClientRequestContext restClientRequestContext) {
        this.restClientRequestContext = restClientRequestContext;
        return this;
    }

    public ClientRestResponseBuilderImpl<T> entityStream(InputStream entityStream) {
        this.entityStream = entityStream;
        return this;
    }

    @Override
    protected AbstractRestResponseBuilder<T> doClone() {
        return new ClientRestResponseBuilderImpl<>();
    }

    @Override
    public RestResponseImpl<T> build() {
        ClientRestResponseImpl<T> response = new ClientRestResponseImpl<>();
        populateResponse(response);
        response.restClientRequestContext = restClientRequestContext;
        response.setEntityStream(entityStream);
        return response;
    }

    @Override
    public RestResponse.ResponseBuilder<T> contentLocation(URI location) {
        //TODO: needs some thinking
        throw new NotImplementedYet();
    }

    @Override
    public RestResponse.ResponseBuilder<T> location(URI location) {
        throw new NotImplementedYet();
    }
}
