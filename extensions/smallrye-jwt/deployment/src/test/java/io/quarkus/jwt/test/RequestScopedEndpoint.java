package io.quarkus.jwt.test;

import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

/**
 * An endpoint with explicit {@linkplain RequestScoped} scoping
 */
@Path("/endp-requestscoped")
@RequestScoped
public class RequestScopedEndpoint {
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
                .add("authScheme", context.getAuthenticationScheme())
                .build();
        return result;
    }
}
