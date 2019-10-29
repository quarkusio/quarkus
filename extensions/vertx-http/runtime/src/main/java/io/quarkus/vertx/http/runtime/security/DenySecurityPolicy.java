package io.quarkus.vertx.http.runtime.security;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

public class DenySecurityPolicy implements HttpSecurityPolicy {

    public static final DenySecurityPolicy INSTANCE = new DenySecurityPolicy();

    @Override
    public CompletionStage<CheckResult> checkPermission(RoutingContext request, SecurityIdentity identity,
            AuthorizationRequestContext requestContext) {
        return CompletableFuture.completedFuture(CheckResult.DENY);
    }
}
