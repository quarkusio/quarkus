package io.quarkus.rest.runtime.client;

import java.io.InputStream;

import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponse;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponseBuilder;

public class QuarkusRestClientResponseBuilder extends QuarkusRestResponseBuilder {

    InputStream entityStream;
    RestClientRequestContext restClientRequestContext;

    public QuarkusRestClientResponseBuilder invocationState(RestClientRequestContext restClientRequestContext) {
        this.restClientRequestContext = restClientRequestContext;
        return this;
    }

    public QuarkusRestClientResponseBuilder entityStream(InputStream entityStream) {
        this.entityStream = entityStream;
        return this;
    }

    @Override
    public QuarkusRestResponse build() {
        QuarkusRestClientResponse response = new QuarkusRestClientResponse();
        populateResponse(response);
        response.restClientRequestContext = restClientRequestContext;
        response.setEntityStream(entityStream);
        return response;
    }
}
