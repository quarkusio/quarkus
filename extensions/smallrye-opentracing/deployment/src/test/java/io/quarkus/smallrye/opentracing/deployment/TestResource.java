package io.quarkus.smallrye.opentracing.deployment;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
    public Response faultTolerance() {
        String ret = service.faultTolerance();
        return Response.ok(ret).build();
    }
}
