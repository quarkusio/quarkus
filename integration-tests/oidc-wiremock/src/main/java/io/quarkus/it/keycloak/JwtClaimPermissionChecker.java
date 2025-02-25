package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.PermissionChecker;

@ApplicationScoped
public class JwtClaimPermissionChecker {

    @Inject
    JsonWebToken jwtAccessToken;

    @PermissionChecker("admin-preferred-username")
    boolean preferredUsernameIsAdmin(String fail) {
        if (Boolean.parseBoolean(fail)) {
            return false;
        }
        return "admin".equals(jwtAccessToken.getClaim("preferred_username"));
    }
}
