package io.quarkus.smallrye.opentracing.deployment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public interface RestService {

    @GET
    @Path("/hello")
    Response hello();

    @GET
    @Path("/cdi")
    Response cdi();

    @GET
    @Path("/restClient")
    Response restClient();
}
