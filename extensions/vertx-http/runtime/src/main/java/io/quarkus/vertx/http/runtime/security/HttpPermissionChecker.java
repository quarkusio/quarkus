package io.quarkus.vertx.http.runtime.security;

import java.util.concurrent.CompletionStage;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public interface HttpPermissionChecker {

    /**
     * The priority of the permission checker. Permission checkers are evaluated from highest to lowest
     * priority.
     *
     * @return The priority
     */
    int getPriority();

    CompletionStage<CheckResult> checkPermission(HttpServerRequest request, SecurityIdentity identity);

    /**
     * The results of a permission check
     */
    enum CheckResult {
        /**
         * This permission checker essentially has nothing to say about this request,
         * it will be delegated to the next checker in the chain.
         */
        IGNORE,
        /**
         * If this is returned then the request is allowed, and no further permission checkers will be
         * consulted.
         */
        PERMIT,
        /**
         * Denies the request. If the {@link SecurityIdentity} represents the anonymous user then
         * {@link HttpAuthenticationMechanism#sendChallenge(RoutingContext)} will be invoked, otherwise
         * a 403 forbidden error code will be returned.
         */
        DENY
    }

}
