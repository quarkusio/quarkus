package io.quarkus.oidc.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.arc.AlternativePriority;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.UserInfo;
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
        IdTokenCredential cred = identity.getCredential(IdTokenCredential.class);
        if (cred == null || cred.getToken() == null) {
            LOG.trace("IdToken is null");
            cred = new IdTokenCredential();
        }
        return cred;
    }

    @Produces
    @RequestScoped
    @AlternativePriority(1)
    AccessTokenCredential currentAccessToken() {
        AccessTokenCredential cred = identity.getCredential(AccessTokenCredential.class);
        if (cred == null || cred.getToken() == null) {
            LOG.trace("AccessToken is null");
            cred = new AccessTokenCredential();
        }
        return cred;
    }

    @Produces
    @RequestScoped
    RefreshToken currentRefreshToken() {
        RefreshToken cred = identity.getCredential(RefreshToken.class);
        if (cred == null) {
            LOG.trace("RefreshToken is null");
            cred = new RefreshToken();
        }
        return cred;
    }

    /**
     * The producer method for the current UserInfo
     *
     * @return the user info
     */
    @Produces
    @RequestScoped
    UserInfo currentUserInfo() {
        UserInfo userInfo = (UserInfo) identity.getAttribute(OidcUtils.USER_INFO_ATTRIBUTE);
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
        TokenIntrospection introspection = (TokenIntrospection) identity.getAttribute(OidcUtils.INTROSPECTION_ATTRIBUTE);
        if (introspection == null) {
            LOG.trace("TokenIntrospection is null");
            introspection = new TokenIntrospection();
        }
        return introspection;
    }
}
