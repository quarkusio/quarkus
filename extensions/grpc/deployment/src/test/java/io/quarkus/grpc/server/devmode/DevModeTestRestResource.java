package io.quarkus.grpc.server.devmode;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
