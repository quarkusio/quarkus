package io.quarkus.smallrye.jwt.runtime.auth;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtContext;
import org.wildfly.security.auth.realm.token.TokenValidator;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.evidence.BearerTokenEvidence;

import io.smallrye.jwt.auth.principal.DefaultJWTTokenParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.ParseException;

/**
 * Validates a bearer token according to the MP-JWT rules
 */
@ApplicationScoped
public class MpJwtValidator implements TokenValidator {
    @Inject
    JWTAuthContextInfo authContextInfo;
    private DefaultJWTTokenParser parser = new DefaultJWTTokenParser();

    public MpJwtValidator() {
    }

    public MpJwtValidator(JWTAuthContextInfo authContextInfo) {
        this.authContextInfo = authContextInfo;
    }

    @Override
    public Attributes validate(BearerTokenEvidence evidence) throws RealmUnavailableException {
        JwtClaims claimsSet = validateClaimsSet(evidence.getToken());
        return new ClaimAttributes(claimsSet);
    }

    private JwtClaims validateClaimsSet(String token) throws RealmUnavailableException {
        try {
            JwtContext jwtContext = parser.parse(token, authContextInfo);
            return jwtContext.getJwtClaims();
        } catch (ParseException e) {
            throw new RealmUnavailableException("Failed to verify token", e);
        }
    }
}
