package io.quarkus.grpc.server.devmode;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.quarkus.grpc.runtime.GrpcServerRecorder;

@Path("/grpc-status")
@ApplicationScoped
public class IsUpEndpoint {
    @GET
    public Response isGrpcUp() {
        return GrpcServerRecorder.getVerticleCount() > 0 ? Response.ok().build() : Response.noContent().build();
    }

}
