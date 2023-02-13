package io.quarkus.rest.client.reactive;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

public class TestRestClientListener implements RestClientListener {

    public static final String HEADER_PARAM_NAME = "filterheader";
    public static final String HEADER_PARAM_VALUE = "present";

    @Override
    public void onNewClient(Class<?> aClass, RestClientBuilder restClientBuilder) {
        restClientBuilder.register(new TestRestClientFilter());
    }

    static class TestRestClientFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext clientRequestContext) {
            clientRequestContext.getHeaders().putSingle(HEADER_PARAM_NAME, HEADER_PARAM_VALUE);
        }
    }
}
