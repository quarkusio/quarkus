package io.quarkus.it.keycloak;

import static org.eclipse.microprofile.jwt.Claims.iss;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.TenantFeature;
import io.smallrye.jwt.auth.ClaimsValidator;
import io.smallrye.jwt.common.JwtClaims;

@Unremovable
@ApplicationScoped
@TenantFeature("tenant-public-key")
public class IssuerValidator implements ClaimsValidator {

    @Override
    public String validate(VerificationContext context) {
        JwtClaims claims = context.claims();
        if (claims.hasClaim(iss.name())
                && "unacceptable-issuer".equals((String) claims.getClaim(iss.name()))) {
            // issuer matched
            return "The 'unacceptable-issuer' is not allowed";
        }
        return null;
    }
}
