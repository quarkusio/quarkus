package io.quarkus.it.keycloak;

import jakarta.enterprise.context.Dependent;

import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;

import io.quarkus.arc.Unremovable;

@Unremovable
@Dependent
public class GlobalJwtPreferredNameValidator implements Validator {

    @Override
    public String validate(JwtContext jwtContext) throws MalformedClaimException {
        if (jwtContext.getJwtClaims().hasClaim("preferred_username")
                && jwtContext.getJwtClaims().isClaimValueString("preferred_username")
                && jwtContext.getJwtClaims().getClaimValueAsString("preferred_username").contains("jdoe")) {
            return "scope validation failed, the 'fail-validation' scope is not allowed";
        }
        return null;
    }
}
