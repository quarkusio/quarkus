package io.quarkus.hibernate.validator.runtime.jaxrs;

import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;

import io.quarkus.arc.DefaultBean;

@Singleton
@DefaultBean
public class ResteasyReactiveContextLocaleResolver extends AbstractLocaleResolver {

    private final HttpHeaders headers;

    // automatically injected for RESTEasy Reactive because of org.jboss.resteasy.reactive.server.injection.ContextProducers
    public ResteasyReactiveContextLocaleResolver(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    protected HttpHeaders getHeaders() {
        try {
            headers.getLength(); // this forces the creation of the actual object which will fail if there is no request in flight
            return headers;
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
