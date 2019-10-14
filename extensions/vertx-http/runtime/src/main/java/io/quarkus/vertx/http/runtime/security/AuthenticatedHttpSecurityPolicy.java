package io.quarkus.vertx.http.runtime.security;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.HttpServerRequest;

/**
 * permission checker that checks if the user is authenticated
 */
public class AuthenticatedHttpSecurityPolicy implements HttpSecurityPolicy {

    @Override
    public CompletionStage<CheckResult> checkPermission(HttpServerRequest request, SecurityIdentity identity) {
        return CompletableFuture.completedFuture(identity.isAnonymous() ? CheckResult.DENY : CheckResult.PERMIT);
    }
}
