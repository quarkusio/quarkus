package io.quarkus.vertx.http.runtime.security;

import java.util.function.Function;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * permission checker that checks if the user is authenticated
 */
public class AuthenticatedHttpSecurityPolicy implements HttpSecurityPolicy {

    public static final String NAME = "authenticated";

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return identity.map(new Function<SecurityIdentity, CheckResult>() {
            @Override
            public CheckResult apply(SecurityIdentity identity) {
                return identity.isAnonymous() ? CheckResult.DENY : CheckResult.PERMIT;
            }
        });
    }
}
