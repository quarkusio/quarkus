package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.runtime.OidcConfig;
import io.smallrye.jwt.auth.ClaimsValidator;
import io.smallrye.jwt.common.JwtClaims;

@Unremovable
@TenantFeature("tenant-requiredclaim")
@ApplicationScoped
public class TenantSpecificJwtPreferredNameValidator implements ClaimsValidator {

    private final String requiredClaim;

    public TenantSpecificJwtPreferredNameValidator(OidcConfig oidcConfig) {
        this.requiredClaim = oidcConfig.namedTenants().get("tenant-requiredclaim").token().requiredClaims().get("azp")
                .iterator().next();
    }

    @Override
    public String validate(VerificationContext context) {
        JwtClaims claims = context.claims();
        // verify that normal scoped validator is created when the runtime config is ready
        if (!"quarkus-app-b".equals(requiredClaim)) {
            throw new IllegalStateException("The 'tenant-requiredclaim' tenant required claim 'azp' is not 'quarkus-app-b'");
        }

        if (claims.hasClaim("preferred_username")
                && claims.getClaim("preferred_username") instanceof String
                && ((String) claims.getClaim("preferred_username")).contains("admin")) {
            return "scope validation failed, the 'fail-validation' scope is not allowed";
        }
        return null;
    }
}
