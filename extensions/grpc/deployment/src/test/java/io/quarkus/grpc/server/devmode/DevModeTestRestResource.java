package io.quarkus.grpc.server.devmode;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/test")
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
public class DevModeTestRestResource {

    @Inject
    DevModeTestInterceptor interceptor;

    @GET
    public String get() {
        return "testresponse";
    }

    @GET
    @Path("/interceptor-status")
    public String getInterceptorStatus() {
        return interceptor.getLastStatus();
    }
}
