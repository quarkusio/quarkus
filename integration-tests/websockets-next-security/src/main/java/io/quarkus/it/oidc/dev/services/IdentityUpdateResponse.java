package io.quarkus.it.oidc.dev.services;

public record IdentityUpdateResponse(String message,
        String identityPrincipal,
        Long expiresAt) {
}
