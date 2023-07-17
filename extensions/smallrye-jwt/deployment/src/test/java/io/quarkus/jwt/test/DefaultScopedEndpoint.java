package io.quarkus.jwt.test;

import java.util.Optional;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

/**
 * An endpoint that uses no explicit scoping
 */
@Path("/endp-defaultscoped")
public class DefaultScopedEndpoint {
    @Inject
    @Claim(standard = Claims.preferred_username)
    Optional<JsonString> currentUsername;
    @Context
    private SecurityContext context;

    /**
     * Validate that the passed in username parameter matches the injected preferred_username claim
     *
     * @param username - expected username
     * @return test result response
     */
    @GET
    @Path("/validateUsername")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject validateUsername(@QueryParam("username") String username) {
        boolean pass = false;
        String msg;
        if (!currentUsername.isPresent()) {
            msg = "Injected preferred_username value is null, FAIL";
        } else if (currentUsername.get().getString().equals(username)) {
            msg = "\nInjected Principal#getName matches, PASS";
            pass = true;
        } else {
            msg = String.format("Injected preferred_username %s != %s, FAIL", currentUsername.get().getString(), username);
        }

        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }
}
