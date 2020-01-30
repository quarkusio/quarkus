package io.quarkus.oidc.runtime;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;

abstract class AbstractOidcAuthenticationMechanism implements HttpAuthenticationMechanism {

    protected static final String BEARER = "Bearer";

    @Inject
    DefaultTenantConfigResolver tenantConfigResolver;

    protected CompletionStage<SecurityIdentity> authenticate(IdentityProviderManager identityProviderManager,
            TokenCredential token) {
        return identityProviderManager.authenticate(new TokenAuthenticationRequest(token));
    }
}
