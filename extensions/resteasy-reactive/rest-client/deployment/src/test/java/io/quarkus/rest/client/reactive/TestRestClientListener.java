package io.quarkus.rest.client.reactive;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

public class TestRestClientListener implements RestClientListener {

    public static final String HEADER_PARAM_VALUE = "present";

    @Override
    public void onNewClient(Class<?> aClass, RestClientBuilder restClientBuilder) {
        String headerPropertyName = CDI.current().select(TestHeaderConfig.class).get().getHeaderPropertyName();
        restClientBuilder.register(new TestRestClientFilter(headerPropertyName));
    }

    static class TestRestClientFilter implements ClientRequestFilter {

        private final String headerPropertyName;

        public TestRestClientFilter(String headerPropertyName) {
            this.headerPropertyName = headerPropertyName;
        }

        @Override
        public void filter(ClientRequestContext clientRequestContext) {
            clientRequestContext.getHeaders().putSingle(headerPropertyName, HEADER_PARAM_VALUE);
        }
    }
}
