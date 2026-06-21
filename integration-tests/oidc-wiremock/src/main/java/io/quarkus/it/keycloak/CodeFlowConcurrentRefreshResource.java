package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@Path("/code-flow-concurrent-refresh")
@Authenticated
public class CodeFlowConcurrentRefreshResource {

    @GET
    public Uni<String> access() {
        return Uni.createFrom().item("hello").emitOn(Infrastructure.getDefaultExecutor());
    }
}
