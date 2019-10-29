package io.quarkus.oidc.runtime;

import java.util.concurrent.CompletionStage;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.vertx.ext.auth.oauth2.OAuth2Auth;

abstract class AbstractOidcAuthenticationMechanism implements HttpAuthenticationMechanism {

    protected static final String BEARER = "Bearer";

    protected volatile OAuth2Auth auth;
    protected OidcConfig config;

    public AbstractOidcAuthenticationMechanism setAuth(OAuth2Auth auth, OidcConfig config) {
        this.auth = auth;
        this.config = config;
        return this;
    }

    protected CompletionStage<SecurityIdentity> authenticate(IdentityProviderManager identityProviderManager,
            TokenCredential token) {
        return identityProviderManager.authenticate(new TokenAuthenticationRequest(token));
    }
}
