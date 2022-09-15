package io.quarkus.oidc.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.auth.cdi.NullJsonWebToken;

@Priority(2)
@Alternative
@RequestScoped
public class OidcJsonWebTokenProducer {
    private static final Logger LOG = Logger.getLogger(OidcJsonWebTokenProducer.class);

    @Inject
    SecurityIdentity identity;

    /**
     * The producer method for the current access token
     *
     * @return the access token
     */
    @Produces
    @RequestScoped
    JsonWebToken currentAccessToken() {
        return getTokenCredential(AccessTokenCredential.class);
    }

    /**
     * The producer method for the current id token
     *
     * @return the id token
     */
    @Produces
    @IdToken
    @RequestScoped
    JsonWebToken currentIdToken() {
        return getTokenCredential(IdTokenCredential.class);
    }

    private JsonWebToken getTokenCredential(Class<? extends TokenCredential> type) {
        if (identity.isAnonymous()) {
            return new NullJsonWebToken();
        }
        if (identity.getPrincipal() instanceof OidcJwtCallerPrincipal
                && ((OidcJwtCallerPrincipal) identity.getPrincipal()).getCredential().getClass() == type) {
            return (JsonWebToken) identity.getPrincipal();
        }
        TokenCredential credential = identity.getCredential(type);
        if (credential != null && credential.getToken() != null) {
            if (credential instanceof AccessTokenCredential && ((AccessTokenCredential) credential).isOpaque()) {
                throw new OIDCException("Opaque access token can not be converted to JsonWebToken");
            }
            JwtClaims jwtClaims;
            try {
                jwtClaims = new JwtConsumerBuilder()
                        .setSkipSignatureVerification()
                        .setSkipAllValidators()
                        .build().processToClaims(credential.getToken());
            } catch (InvalidJwtException e) {
                throw new OIDCException(e);
            }
            jwtClaims.setClaim(Claims.raw_token.name(), credential.getToken());
            return new OidcJwtCallerPrincipal(jwtClaims, credential);
        }
        String tokenType = type == AccessTokenCredential.class ? "access" : "ID";
        LOG.tracef("Current identity is not associated with an %s token", tokenType);
        return new NullJsonWebToken();
    }
}
