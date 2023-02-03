package io.quarkus.grpc.server.devmode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.quarkus.grpc.runtime.GrpcServerRecorder;

@Path("/grpc-status")
@ApplicationScoped
public class IsUpEndpoint {
    @GET
    public Response isGrpcUp() {
        return GrpcServerRecorder.getVerticleCount() > 0 ? Response.ok().build() : Response.noContent().build();
    }

}
