package io.quarkus.oidc.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.security.identity.SecurityIdentity;

@RequestScoped
public class OidcTokenCredentialProducer {

    @Inject
    SecurityIdentity identity;

    /**
     * The producer method for the current id token
     *
     * @return the id token
     */
    @Produces
    @RequestScoped
    IdTokenCredential currentIdToken() {
        return identity.getCredential(IdTokenCredential.class);
    }

    @Produces
    @RequestScoped
    AccessTokenCredential currentAccessToken() {
        return identity.getCredential(AccessTokenCredential.class);
    }

    @Produces
    @RequestScoped
    RefreshToken currentRefreshToken() {
        return identity.getCredential(RefreshToken.class);
    }
}
