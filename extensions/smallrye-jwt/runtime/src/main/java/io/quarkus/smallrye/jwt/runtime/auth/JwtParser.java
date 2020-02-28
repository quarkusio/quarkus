package io.quarkus.smallrye.jwt.runtime.auth;

import org.jose4j.jwt.consumer.JwtContext;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.ParseException;

public interface JwtParser {
    JwtContext parse(String token, JWTAuthContextInfo authContextInfo) throws ParseException;
}
