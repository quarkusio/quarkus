package io.quarkus.jaxrs.client.reactive.runtime;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;
import org.jboss.resteasy.reactive.client.impl.ClientResponseBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.ClientRestResponseBuilderImpl;
import org.jboss.resteasy.reactive.common.core.ResponseBuilderFactory;

/**
 * ResponseBuilderFactory used only when the server response builder factory is not available
 */
public class ClientResponseBuilderFactory implements ResponseBuilderFactory {
    @Override
    public Response.ResponseBuilder create() {
        return new ClientResponseBuilderImpl();
    }

    @Override
    public int priority() {
        return 10; // lower than the server one
    }

    @Override
    public <T> ResponseBuilder<T> createRestResponse() {
        return new ClientRestResponseBuilderImpl<>();
    }
}
