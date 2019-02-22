package io.quarkus.smallrye.jwt.runtime.auth;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.undertow.security.idm.Credential;

/**
 * This is an implementation of the undertow Credential that wraps the bearer token and configured JWTAuthContextInfo
 * needed for validation of the token.
 */
public class JWTCredential implements Credential {
    private JWTAuthContextInfo authContextInfo;

    private String bearerToken;

    private String name;

    private Exception jwtException;

    /**
     * @param bearerToken
     * @param authContextInfo
     */
    public JWTCredential(String bearerToken, JWTAuthContextInfo authContextInfo) {
        this.bearerToken = bearerToken;
        this.authContextInfo = authContextInfo;
    }

    /**
     * This just parses the token without validation to extract one of the following in order to obtain
     * the name to be used for the principal:
     * upn
     * preferred_username
     * subject
     *
     * If there is an exception it sets the name to INVALID_TOKEN_NAME and saves the exception for access
     * via {@link #getJwtException()}
     *
     * @return the name to use for the principal
     */
    public String getName() {
        if (name == null) {
            name = "INVALID_TOKEN_NAME";
            try {
                // Build a JwtConsumer that doesn't check signatures or do any validation.
                JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
                        .setSkipAllValidators()
                        .setDisableRequireSignature()
                        .setSkipSignatureVerification()
                        .build();

                //The first JwtConsumer is basically just used to parse the JWT into a JwtContext object.
                JwtContext jwtContext = firstPassJwtConsumer.process(bearerToken);
                JwtClaims claimsSet = jwtContext.getJwtClaims();
                // We have to determine the unique name to use as the principal name. It comes from upn, preferred_username, sub in that order
                name = claimsSet.getClaimValue("upn", String.class);
                if (name == null) {
                    name = claimsSet.getClaimValue("preferred_username", String.class);
                    if (name == null) {
                        name = claimsSet.getSubject();
                    }
                }
            } catch (Exception e) {
                jwtException = e;
            }
        }
        return name;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public JWTAuthContextInfo getAuthContextInfo() {
        return authContextInfo;
    }

    public Exception getJwtException() {
        return jwtException;
    }
}
