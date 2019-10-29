package io.quarkus.vertx.http.runtime.security;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

public class PermitSecurityPolicy implements HttpSecurityPolicy {

    @Override
    public CompletionStage<CheckResult> checkPermission(RoutingContext request, SecurityIdentity identity,
            AuthorizationRequestContext requestContext) {
        return CompletableFuture.completedFuture(CheckResult.PERMIT);
    }
}
