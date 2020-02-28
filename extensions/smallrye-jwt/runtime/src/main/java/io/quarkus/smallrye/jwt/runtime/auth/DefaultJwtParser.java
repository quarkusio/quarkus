package io.quarkus.smallrye.jwt.runtime.auth;

import javax.enterprise.context.ApplicationScoped;

import org.jose4j.jwt.consumer.JwtContext;

import io.quarkus.arc.DefaultBean;
import io.smallrye.jwt.auth.principal.DefaultJWTTokenParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.ParseException;

@DefaultBean
@ApplicationScoped
public class DefaultJwtParser implements JwtParser {
    private final DefaultJWTTokenParser parser = new DefaultJWTTokenParser();

    @Override
    public JwtContext parse(String token, JWTAuthContextInfo authContextInfo) throws ParseException {
        return parser.parse(token, authContextInfo);
    }
}
