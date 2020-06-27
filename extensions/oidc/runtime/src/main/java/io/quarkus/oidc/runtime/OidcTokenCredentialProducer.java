package io.quarkus.oidc.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.oidc.UserInfo;
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

    /**
     * The producer method for the current UserInfo
     *
     * @return the user info
     */
    @Produces
    @RequestScoped
    UserInfo currentUserInfo() {
        UserInfo userInfo = (UserInfo) identity.getAttribute("userinfo");
        if (userInfo == null) {
            throw new OIDCException("UserInfo can not be injected");
        }
        return userInfo;
    }
}
