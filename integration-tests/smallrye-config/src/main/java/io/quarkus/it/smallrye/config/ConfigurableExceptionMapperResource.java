package io.quarkus.it.smallrye.config;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/exception")
@Produces(MediaType.WILDCARD)
public class ConfigurableExceptionMapperResource {
    @GET
    public Response get() {
        throw new ConfigurableExceptionMapper.ConfigurableExceptionMapperException();
    }
}
