package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.spi.runtime.AuthorizationController;

/**
 * Class that is responsible for running the HTTP based permission checks
 */
@Singleton
public class HttpAuthorizer extends AbstractHttpAuthorizer {

    HttpAuthorizer(HttpAuthenticator httpAuthenticator, IdentityProviderManager identityProviderManager,
            AuthorizationController controller, Instance<HttpSecurityPolicy> installedPolicies) {
        super(httpAuthenticator, identityProviderManager, controller, toList(installedPolicies));
    }

    private static List<HttpSecurityPolicy> toList(Instance<HttpSecurityPolicy> installedPolicies) {
        List<HttpSecurityPolicy> policies = new ArrayList<>();
        for (HttpSecurityPolicy i : installedPolicies) {
            policies.add(i);
        }
        return policies;
    }
}
