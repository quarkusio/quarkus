package io.quarkus.oidc.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

@ApplicationScoped
public class PrimitiveTypeClaimApplicationScopedEndpoint {
    @Inject
    @Claim(standard = Claims.upn)
    String upn;

    @GET
    public String get() {
        return "hello";
    }
}
