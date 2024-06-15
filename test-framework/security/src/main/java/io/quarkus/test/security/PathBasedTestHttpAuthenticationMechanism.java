package io.quarkus.test.security;

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

/**
 * When authentication mechanism is selected with the {@link TestSecurity#authMechanism()} annotation attribute,
 * we must be sure that the test mechanism is primary identity provider for that authentication type.
 * <p>
 * For example when a test method is annotated with `@TestSecurity(authMechanism = "basic")`,
 * we want to be the ones providing basic authentication when no authorization headers are present,
 * and not the {@link io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism} mechanism.
 * This test mechanism must exist because when a path-specific authentication mechanism is selected,
 * for example via {@link io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication},
 * it is also required and therefore exactly one mechanism is enforced.
 */
@ApplicationScoped
public class PathBasedTestHttpAuthenticationMechanism extends AbstractTestHttpAuthenticationMechanism {

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        if (authMechanism != null && requestNotAuthenticated(context)) {
            // return the SecurityIdentity defined via @TestSecurity
            return super.authenticate(context, identityProviderManager);
        }
        // do not authenticate - give a change to other mechanisms
        return Uni.createFrom().nullItem();
    }

    @Override
    public int getPriority() {
        return 3000;
    }

    private static boolean requestNotAuthenticated(RoutingContext context) {
        // on a best-effort basis try to guess whether incoming request is authorized
        return context.request().getHeader(AUTHORIZATION) == null
                && !hasOidcSessionCookieCandidate(context);
    }

    private static boolean hasOidcSessionCookieCandidate(RoutingContext context) {
        if (context.request().cookies() == null) {
            return false;
        }
        for (Cookie cookie : context.request().cookies()) {
            if (cookie.getName() != null && cookie.getName().startsWith("q_session")) {
                // there is a possibility this is an OIDC session cookie
                return true;
            }
        }
        return false;
    }
}
