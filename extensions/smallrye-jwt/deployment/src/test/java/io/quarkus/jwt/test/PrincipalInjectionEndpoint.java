package io.quarkus.jwt.test;

import java.security.Principal;
import java.util.Optional;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
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
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Validate that the injection of a {@linkplain Principal} works when using the MP-JWT feature.
 * This validates that the MP-JWT implementation is not interfering with the CDI built in
 * Principal bean.
 * This also validates that the {@linkplain SecurityContext#getUserPrincipal()} is also an
 * instance of the {@linkplain JsonWebToken} interface.
 */
@Path("/endp")
@RequestScoped
@RolesAllowed("Tester")
public class PrincipalInjectionEndpoint {

    @Inject
    Principal principal;
    @Inject
    @Claim(standard = Claims.preferred_username)
    Optional<JsonString> currentUsername;
    @Context
    private SecurityContext context;

    @GET
    @Path("/verifyInjectedPrincipal")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyInjectedPrincipal() {
        boolean pass = false;
        String msg;
        // Validate that the context principal is a JsonWebToken
        Principal jwtPrincipal = context.getUserPrincipal();
        if (jwtPrincipal == null) {
            msg = "SecurityContext#principal value is null, FAIL";
        } else if (jwtPrincipal instanceof JsonWebToken) {
            msg = "SecurityContext#getUserPrincipal is JsonWebToken, PASS";
            pass = true;
        } else {
            msg = String.format("principal: JsonWebToken != %s", jwtPrincipal.getClass().getCanonicalName());
        }
        // Validate that the injection built-in principal name matches the JsonWebToken name
        if (pass) {
            pass = false;
            if (principal == null) {
                msg = "Injected principal value is null, FAIL";
            } else if (!principal.getName().equals(jwtPrincipal.getName())) {
                msg = "Injected principal#name != jwtPrincipal#name, FAIL";
            } else {
                msg += "\nInjected Principal#getName matches, PASS";
                pass = true;
            }
        }

        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/validateUsername")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
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
