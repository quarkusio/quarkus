package io.quarkus.rest.runtime.client;

import java.io.InputStream;

import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponse;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponseBuilder;

public class QuarkusRestClientResponseBuilder extends QuarkusRestResponseBuilder {

    Serialisers serialisers;
    InputStream entityStream;

    public QuarkusRestClientResponseBuilder serializers(Serialisers serialisers) {
        this.serialisers = serialisers;
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
        response.serialisers = serialisers;
        response.setEntityStream(entityStream);
        return response;
    }
}
