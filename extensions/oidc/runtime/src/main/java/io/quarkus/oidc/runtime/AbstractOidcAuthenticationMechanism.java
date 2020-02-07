package io.quarkus.oidc.runtime;

import java.util.concurrent.CompletionStage;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;

abstract class AbstractOidcAuthenticationMechanism {
    protected CompletionStage<SecurityIdentity> authenticate(IdentityProviderManager identityProviderManager,
            TokenCredential token) {
        return identityProviderManager.authenticate(new TokenAuthenticationRequest(token));
    }
}
