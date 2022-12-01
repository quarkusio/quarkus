package io.quarkus.jwt.test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;

import org.jose4j.jwt.JwtClaims;

import io.quarkus.arc.AlternativePriority;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.ParseException;

@ApplicationScoped
@AlternativePriority(1)
public class TestJWTCallerPrincipalFactory extends JWTCallerPrincipalFactory {

    @Override
    public JWTCallerPrincipal parse(String token, JWTAuthContextInfo authContextInfo) throws ParseException {
        try {
            Thread.sleep(5000);
            String json = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
            return new DefaultJWTCallerPrincipal(JwtClaims.parse(json));
        } catch (Exception ex) {
            throw new ParseException(ex.getMessage());
        }
    }
}
