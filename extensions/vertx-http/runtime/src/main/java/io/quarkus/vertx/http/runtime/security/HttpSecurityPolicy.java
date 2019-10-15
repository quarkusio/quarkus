package io.quarkus.vertx.http.runtime.security;

import java.util.concurrent.CompletionStage;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public interface HttpSecurityPolicy {

    CompletionStage<CheckResult> checkPermission(HttpServerRequest request, SecurityIdentity identity);

    /**
     * The results of a permission check
     */
    enum CheckResult {
        /**
         * If this is returned then the request is allowed. All permission checkers must permit a request for it
         * to proceed.
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
