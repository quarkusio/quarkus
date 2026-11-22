package io.quarkus.it.keycloak;

import jakarta.enterprise.context.Dependent;

import io.quarkus.arc.Unremovable;
import io.smallrye.jwt.auth.ClaimsValidator;
import io.smallrye.jwt.common.JwtClaims;

@Unremovable
@Dependent
public class GlobalJwtPreferredNameValidator implements ClaimsValidator {

    @Override
    public String validate(VerificationContext context) {
        JwtClaims claims = context.claims();
        String preferredUserName = claims.getStringClaim("preferred_username");
        if (preferredUserName != null && preferredUserName.contains("jdoe")) {
            return "scope validation failed, the 'fail-validation' scope is not allowed";
        }
        return null;
    }
}
