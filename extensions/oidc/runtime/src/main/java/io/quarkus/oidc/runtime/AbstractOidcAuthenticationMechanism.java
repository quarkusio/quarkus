package io.quarkus.oidc.runtime;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

abstract class AbstractOidcAuthenticationMechanism {
    protected DefaultTenantConfigResolver resolver;
    private HttpAuthenticationMechanism parent;

    protected Uni<SecurityIdentity> authenticate(IdentityProviderManager identityProviderManager,
            RoutingContext context, TokenCredential token) {
        context.put(HttpAuthenticationMechanism.class.getName(), parent);
        return identityProviderManager.authenticate(HttpSecurityUtils.setRoutingContextAttribute(
                new TokenAuthenticationRequest(token), context));
    }

    void init(HttpAuthenticationMechanism parent, DefaultTenantConfigResolver resolver) {
        this.parent = parent;
        this.resolver = resolver;
    }

}
