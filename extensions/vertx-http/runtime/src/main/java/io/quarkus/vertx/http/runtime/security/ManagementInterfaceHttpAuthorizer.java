package io.quarkus.vertx.http.runtime.security;

import java.util.List;

import jakarta.inject.Singleton;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Class that is responsible for running the Management HTTP based permission checks
 */
@Singleton
public class ManagementInterfaceHttpAuthorizer extends AbstractHttpAuthorizer {

    public ManagementInterfaceHttpAuthorizer(HttpAuthenticator httpAuthenticator,
            IdentityProviderManager identityProviderManager,
            AuthorizationController controller, ManagementPathMatchingHttpSecurityPolicy installedPolicy,
            BlockingSecurityExecutor blockingExecutor) {
        super(httpAuthenticator, identityProviderManager, controller,
                List.of(new HttpSecurityPolicy() {

                    @Override
                    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
                            AuthorizationRequestContext requestContext) {
                        return installedPolicy.checkPermission(request, identity, requestContext);
                    }

                }), blockingExecutor);
    }
}
