package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.runtime.OidcConfig;

@Unremovable
@TenantFeature("tenant-requiredclaim")
@ApplicationScoped
public class TenantSpecificJwtPreferredNameValidator implements Validator {

    private final String requiredClaim;

    public TenantSpecificJwtPreferredNameValidator(OidcConfig oidcConfig) {
        this.requiredClaim = oidcConfig.namedTenants().get("tenant-requiredclaim").token().requiredClaims().get("azp")
                .iterator().next();
    }

    @Override
    public String validate(JwtContext jwtContext) throws MalformedClaimException {
        // verify that normal scoped validator is created when the runtime config is ready
        if (!"quarkus-app-b".equals(requiredClaim)) {
            throw new IllegalStateException("The 'tenant-requiredclaim' tenant required claim 'azp' is not 'quarkus-app-b'");
        }

        if (jwtContext.getJwtClaims().hasClaim("preferred_username")
                && jwtContext.getJwtClaims().isClaimValueString("preferred_username")
                && jwtContext.getJwtClaims().getClaimValueAsString("preferred_username").contains("admin")) {
            return "scope validation failed, the 'fail-validation' scope is not allowed";
        }
        return null;
    }
}
