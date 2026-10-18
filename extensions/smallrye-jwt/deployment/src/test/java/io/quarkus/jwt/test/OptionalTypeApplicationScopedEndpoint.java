package io.quarkus.jwt.test;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonString;
import jakarta.ws.rs.GET;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

@ApplicationScoped
public class OptionalTypeApplicationScopedEndpoint {
    @Inject
    @Claim(standard = Claims.upn)
    Optional<JsonString> upn;

    @GET
    public String get() {
        return "hello";
    }
}
