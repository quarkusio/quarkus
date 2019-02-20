package org.jboss.shamrock.jwt.test;


import java.security.Principal;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

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
        }
        else if (jwtPrincipal instanceof JsonWebToken) {
            msg = "SecurityContext#getUserPrincipal is JsonWebToken, PASS";
            pass = true;
        }
        else {
            msg = String.format("principal: JsonWebToken != %s", jwtPrincipal.getClass().getCanonicalName());
        }
        // Validate that the injection built-in principal name matches the JsonWebToken name
        if(pass) {
            pass = false;
            if (principal == null) {
                msg = "Injected principal value is null, FAIL";
            }
            else if (!principal.getName().equals(jwtPrincipal.getName())) {
                msg = "Injected principal#name != jwtPrincipal#name, FAIL";
            }
            else {
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

}
