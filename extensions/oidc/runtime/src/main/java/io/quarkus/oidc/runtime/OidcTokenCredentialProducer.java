package io.quarkus.oidc.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
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
        IdTokenCredential cred = OidcUtils.getTokenCredential(identity, IdTokenCredential.class);
        if (cred == null || cred.getToken() == null) {
            LOG.trace("IdToken is null");
            cred = new IdTokenCredential();
        }
        return cred;
    }

    @Produces
    @RequestScoped
    @Alternative
    @Priority(1)
    AccessTokenCredential currentAccessToken() {
        AccessTokenCredential cred = OidcUtils.getTokenCredential(identity, AccessTokenCredential.class);
        if (cred == null || cred.getToken() == null) {
            LOG.trace("AccessToken is null");
            cred = new AccessTokenCredential();
        }
        return cred;
    }

    @Produces
    @RequestScoped
    RefreshToken currentRefreshToken() {
        RefreshToken cred = OidcUtils.getTokenCredential(identity, RefreshToken.class);
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
        UserInfo userInfo = OidcUtils.getAttribute(identity, OidcUtils.USER_INFO_ATTRIBUTE);
        if (userInfo == null) {
            LOG.trace("UserInfo is null");
            userInfo = new UserInfo();
        }
        return userInfo;
    }

    /**
     * The producer method for the ID token TokenIntrospection only.
     *
     * @return the ID token introspection
     */
    @Produces
    @RequestScoped
    @IdToken
    TokenIntrospection idTokenIntrospection() {
        return tokenIntrospectionFromIdentityAttribute();
    }

    /**
     * The producer method for the current TokenIntrospection.
     * <p/>
     * This TokenIntrospection always represents the bearer access token introspection when the bearer access tokens
     * are used.
     * <p/>
     * In case of the authorization code flow, it represents a code flow access token introspection
     * if it has been enabled by setting the `quarkus.oidc.authentication.verify-access-token` property to `true`
     * and an ID token introspection otherwise. Use the `@IdToken` qualifier if both ID and code flow access tokens
     * must be introspected.
     *
     * @return the token introspection
     */
    @Produces
    @RequestScoped
    TokenIntrospection tokenIntrospection() {
        TokenVerificationResult codeFlowAccessTokenResult = OidcUtils.getAttribute(identity,
                OidcUtils.CODE_ACCESS_TOKEN_RESULT);
        if (codeFlowAccessTokenResult == null) {
            return tokenIntrospectionFromIdentityAttribute();
        } else {
            return codeFlowAccessTokenResult.introspectionResult;
        }
    }

    TokenIntrospection tokenIntrospectionFromIdentityAttribute() {
        TokenIntrospection introspection = OidcUtils.getAttribute(identity, OidcUtils.INTROSPECTION_ATTRIBUTE);
        if (introspection == null) {
            LOG.trace("TokenIntrospection is null");
            introspection = new TokenIntrospection();
        }
        return introspection;
    }

}
