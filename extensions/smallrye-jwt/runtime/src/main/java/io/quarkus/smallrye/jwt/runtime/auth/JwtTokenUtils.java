package io.quarkus.smallrye.jwt.runtime.auth;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;

@ApplicationScoped
public class JwtTokenUtils {
    public JsonWebToken decodeTokenUnverified(String tokenString) throws InvalidJwtException {
        JwtConsumer consumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();

        JwtClaims claimsSet = consumer.processToClaims(tokenString);
        return new DefaultJWTCallerPrincipal(tokenString, claimsSet);
    }
}
