package io.quarkus.grpc.server.devmode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.quarkus.arc.Arc;
import io.quarkus.grpc.runtime.GrpcContainer;

@Path("/grpc-status")
@ApplicationScoped
public class IsUpEndpoint {
    @GET
    public Response isGrpcUp() {
        // The GrpcContainer bean is only registered when the gRPC server is started
        if (Arc.container().instance(GrpcContainer.class).isAvailable()) {
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

}
