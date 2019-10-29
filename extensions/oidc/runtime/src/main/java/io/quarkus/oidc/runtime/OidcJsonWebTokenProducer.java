package io.quarkus.oidc.runtime;

import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import io.quarkus.oidc.IdToken;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;

@Priority(2)
@Alternative
@RequestScoped
public class OidcJsonWebTokenProducer {

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

    /**
     * The producer method for the current id token
     *
     * @return the id token
     */
    @Produces
    @RequestScoped
    RefreshToken currentRefreshToken() {
        return identity.getCredential(RefreshToken.class);
    }

    private JsonWebToken getTokenCredential(Class<? extends TokenCredential> type) {
        if (identity.isAnonymous()) {
            return new NullJsonWebToken();
        }
        TokenCredential credential = identity.getCredential(type);
        if (credential != null) {
            JwtClaims jwtClaims;
            try {
                jwtClaims = new JwtConsumerBuilder()
                        .setSkipSignatureVerification()
                        .setSkipAllValidators()
                        .build().processToClaims(credential.getToken());
            } catch (InvalidJwtException e) {
                throw new RuntimeException(e);
            }
            return new OidcJwtCallerPrincipal(jwtClaims);
        }
        throw new IllegalStateException("Current identity not associated with an access token");
    }

    private static class NullJsonWebToken implements JsonWebToken {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<String> getClaimNames() {
            return null;
        }

        @Override
        public <T> T getClaim(String claimName) {
            return null;
        }
    }
}
