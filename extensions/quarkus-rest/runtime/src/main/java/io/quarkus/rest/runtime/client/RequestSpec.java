package io.quarkus.rest.runtime.client;

import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;

public class RequestSpec {
    final QuarkusRestConfiguration configuration;
    final ClientRequestHeaders headers;

    boolean chunked;

    public RequestSpec(QuarkusRestConfiguration configuration) {
        this.configuration = configuration;
        headers = new ClientRequestHeaders(configuration);
    }

    public RequestSpec(RequestSpec requestSpec) {
        this.configuration = requestSpec.configuration;
        this.headers = new ClientRequestHeaders(configuration);
        this.headers.headers.putAll(requestSpec.headers.headers);
        this.chunked = requestSpec.chunked;
    }
}
