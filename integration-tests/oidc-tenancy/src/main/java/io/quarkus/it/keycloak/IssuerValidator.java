package io.quarkus.it.keycloak;

import static org.eclipse.microprofile.jwt.Claims.iss;

import jakarta.enterprise.context.ApplicationScoped;

import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.TenantFeature;

@Unremovable
@ApplicationScoped
@TenantFeature("tenant-public-key")
public class IssuerValidator implements Validator {

    @Override
    public String validate(JwtContext jwtContext) throws MalformedClaimException {
        if (jwtContext.getJwtClaims().hasClaim(iss.name())
                && "unacceptable-issuer".equals(jwtContext.getJwtClaims().getClaimValueAsString(iss.name()))) {
            // issuer matched
            return "The 'unacceptable-issuer' is not allowed";
        }
        return null;
    }
}
