package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.vertx.http.security.AuthorizationPolicy
import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path

@AuthorizationPolicy(name = "suspended")
@Path("/secured-class")
class SecuredClassResource {

    @Path("/authorization-policy-suspend")
    @GET
    suspend fun authorizationPolicySuspend() = "Hello from Quarkus REST"

    @PermitAll @Path("/public") @GET suspend fun publicEndpoint() = "Hello to everyone!"
}
