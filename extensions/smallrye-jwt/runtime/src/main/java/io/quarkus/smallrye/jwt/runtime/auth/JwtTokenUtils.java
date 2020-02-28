package io.quarkus.smallrye.jwt.runtime.auth;

import javax.enterprise.context.ApplicationScoped;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

@ApplicationScoped
public class JwtTokenUtils {
    public JwtClaims decodeTokenUnverified(String tokenString) throws InvalidJwtException {
        JwtConsumer consumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();
        return consumer.processToClaims(tokenString);
    }
}
