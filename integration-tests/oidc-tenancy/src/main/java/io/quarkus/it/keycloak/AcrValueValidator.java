package io.quarkus.it.keycloak;

import static org.eclipse.microprofile.jwt.Claims.acr;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationFailedException;

@Unremovable
@ApplicationScoped
@TenantFeature("step-up-auth-custom-validator")
public class AcrValueValidator implements Validator {

    @Override
    public String validate(JwtContext jwtContext) throws MalformedClaimException {
        var jwtClaims = jwtContext.getJwtClaims();
        if (jwtClaims.hasClaim(acr.name())) {
            var acrClaim = jwtClaims.getStringListClaimValue(acr.name());
            if (acrClaim.contains("delta") && acrClaim.contains("epsilon") && acrClaim.contains("zeta")) {
                return null;
            }
        }
        String requiredAcrValues = "delta,epsilon,zeta";
        throw new AuthenticationFailedException(Map.of(OidcConstants.ACR_VALUES, requiredAcrValues));
    }
}
