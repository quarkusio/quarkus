package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.vertx.http.security.AuthorizationPolicy
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path

@Path("/secured-method")
class SecuredMethodResource {

    @Path("/authorization-policy-suspend")
    @GET
    @AuthorizationPolicy(name = "suspended")
    suspend fun authorizationPolicySuspend() = "Hello from Quarkus REST"
}
