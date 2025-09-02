package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy.CheckResult
import io.smallrye.mutiny.Uni
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class SuspendAuthorizationPolicy : HttpSecurityPolicy {
    override fun checkPermission(
        request: RoutingContext?,
        identity: Uni<SecurityIdentity>?,
        requestContext: HttpSecurityPolicy.AuthorizationRequestContext?,
    ): Uni<CheckResult> {
        val authZHeader = request?.request()?.getHeader(HttpHeaders.AUTHORIZATION)
        if (authZHeader == "you-can-trust-me") {
            return CheckResult.permit()
        }
        return CheckResult.deny()
    }

    override fun name() = "suspended"
}
