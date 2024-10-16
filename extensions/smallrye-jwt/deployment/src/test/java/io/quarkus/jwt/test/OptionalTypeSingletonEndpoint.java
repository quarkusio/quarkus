package io.quarkus.jwt.test;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.json.JsonString;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

@Path("/endpoint")
public class OptionalTypeSingletonEndpoint {
    @Inject
    @Claim(standard = Claims.upn)
    Optional<JsonString> upn;

    @GET
    public String get() {
        return "hello";
    }
}
