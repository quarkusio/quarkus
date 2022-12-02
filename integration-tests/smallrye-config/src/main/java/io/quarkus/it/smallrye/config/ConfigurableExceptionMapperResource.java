package io.quarkus.it.smallrye.config;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/exception")
@Produces(MediaType.WILDCARD)
public class ConfigurableExceptionMapperResource {
    @GET
    public Response get() {
        throw new ConfigurableExceptionMapper.ConfigurableExceptionMapperException();
    }
}
