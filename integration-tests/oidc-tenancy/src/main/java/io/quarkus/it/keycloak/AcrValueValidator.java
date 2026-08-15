package io.quarkus.it.keycloak;

import static org.eclipse.microprofile.jwt.Claims.acr;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationFailedException;
import io.smallrye.jwt.auth.ClaimsValidator;
import io.smallrye.jwt.common.JwtClaims;

@Unremovable
@ApplicationScoped
@TenantFeature("step-up-auth-custom-validator")
public class AcrValueValidator implements ClaimsValidator {

    @Override
    public String validate(VerificationContext context) {
        JwtClaims claims = context.claims();
        if (claims.getClaim(acr.name()) instanceof List<?> acrClaim
                && acrClaim.contains("delta") && acrClaim.contains("epsilon") && acrClaim.contains("zeta")) {
            return null;
        }
        String requiredAcrValues = "delta,epsilon,zeta";
        throw new AuthenticationFailedException(Map.of(OidcConstants.ACR_VALUES, requiredAcrValues));
    }
}
