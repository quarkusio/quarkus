package org.jboss.resteasy.reactive.client.impl;

import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;

public class RequestSpec {
    final ConfigurationImpl configuration;
    final ClientRequestHeaders headers;

    boolean chunked;

    public RequestSpec(ConfigurationImpl configuration) {
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
