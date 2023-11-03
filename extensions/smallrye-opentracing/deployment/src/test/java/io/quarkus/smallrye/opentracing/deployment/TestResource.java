package io.quarkus.smallrye.opentracing.deployment;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

@Path("/")
public class TestResource implements RestService {

    @Inject
    Service service;

    @Context
    private UriInfo uri;

    @Override
    public Response hello() {
        return Response.ok().build();
    }

    @Override
    public Response cdi() {
        service.foo();
        return Response.ok().build();
    }

    @Override
    public Response restClient() {
        RestService client = RestClientBuilder.newBuilder()
                .baseUri(uri.getBaseUri())
                .build(RestService.class);
        client.hello();
        return Response.ok().build();
    }

    @Override
    public CompletionStage<String> faultTolerance() {
        return service.faultTolerance();
    }

    public List<Fruit> jpa() {
        return service.getFruits();
    }
}
