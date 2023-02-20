package io.quarkus.oidc.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;

@RequestScoped
public class OidcTokenCredentialProducer {
    private static final Logger LOG = Logger.getLogger(OidcTokenCredentialProducer.class);
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
        TokenCredential cred = OidcUtils.getTokenCredential(identity, IdTokenCredential.class);
        if (cred == null || cred.getToken() == null) {
            LOG.trace("IdToken is null");
            cred = new IdTokenCredential();
        }
        return (IdTokenCredential) cred;
    }

    @Produces
    @RequestScoped
    @Alternative
    @Priority(1)
    AccessTokenCredential currentAccessToken() {
        TokenCredential cred = OidcUtils.getTokenCredential(identity, AccessTokenCredential.class);
        if (cred == null || cred.getToken() == null) {
            LOG.trace("AccessToken is null");
            cred = new AccessTokenCredential();
        }
        return (AccessTokenCredential) cred;
    }

    @Produces
    @RequestScoped
    RefreshToken currentRefreshToken() {
        TokenCredential cred = OidcUtils.getTokenCredential(identity, RefreshToken.class);
        if (cred == null) {
            LOG.trace("RefreshToken is null");
            cred = new RefreshToken();
        }
        return (RefreshToken) cred;
    }

    /**
     * The producer method for the current UserInfo
     *
     * @return the user info
     */
    @Produces
    @RequestScoped
    UserInfo currentUserInfo() {
        UserInfo userInfo = OidcUtils.getAttribute(identity, OidcUtils.USER_INFO_ATTRIBUTE);
        if (userInfo == null) {
            LOG.trace("UserInfo is null");
            userInfo = new UserInfo();
        }
        return userInfo;
    }

    /**
     * The producer method for the current UserInfo
     *
     * @return the user info
     */
    @Produces
    @RequestScoped
    TokenIntrospection currentTokenIntrospection() {
        TokenIntrospection introspection = OidcUtils.getAttribute(identity, OidcUtils.INTROSPECTION_ATTRIBUTE);
        if (introspection == null) {
            LOG.trace("TokenIntrospection is null");
            introspection = new TokenIntrospection();
        }
        return introspection;
    }
}
