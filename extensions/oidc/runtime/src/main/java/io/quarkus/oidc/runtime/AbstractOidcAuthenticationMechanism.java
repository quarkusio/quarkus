package io.quarkus.oidc.runtime;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.smallrye.mutiny.Uni;

abstract class AbstractOidcAuthenticationMechanism {
    protected DefaultTenantConfigResolver resolver;

    protected Uni<SecurityIdentity> authenticate(IdentityProviderManager identityProviderManager,
            TokenCredential token) {
        return identityProviderManager.authenticate(new TokenAuthenticationRequest(token));
    }

    void setResolver(DefaultTenantConfigResolver resolver) {
        this.resolver = resolver;
    }

}
