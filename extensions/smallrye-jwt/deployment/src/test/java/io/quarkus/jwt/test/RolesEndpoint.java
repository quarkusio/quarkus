package io.quarkus.jwt.test;

import java.security.Principal;
import java.util.Date;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;

@Path("/endp")
@DenyAll
@RequestScoped
public class RolesEndpoint {

    @Inject
    JsonWebToken jwtPrincipal;

    @Inject
    @Claim("raw_token")
    ClaimValue<String> rawToken;

    @GET
    @Path("/echo")
    @RolesAllowed("Echoer")
    public String echoInput(@Context SecurityContext sec, @QueryParam("input") String input) {
        Principal user = sec.getUserPrincipal();
        return input + ", user=" + user.getName();
    }

    @GET
    @Path("/echo2")
    @RolesAllowed("NoSuchUser")
    public String echoInput2(@Context SecurityContext sec, @QueryParam("input") String input) {
        Principal user = sec.getUserPrincipal();
        String name = user != null ? user.getName() : "<null>";
        return input + ", user=" + name;
    }

    @GET
    @Path("/echoNeedsToken2Role")
    @RolesAllowed("Token2Role")
    public String echoNeedsToken2Role(@Context SecurityContext sec, @QueryParam("input") String input) {
        Principal user = sec.getUserPrincipal();
        return input + ", user=" + user.getName();
    }

    /**
     * Validate that the SecurityContext#getUserPrincipal is a JsonWebToken
     *
     * @param sec
     * @return
     */
    @GET
    @Path("/getPrincipalClass")
    @RolesAllowed("Tester")
    public String getPrincipalClass(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        boolean isJsonWebToken = user instanceof JsonWebToken;
        return "isJsonWebToken:" + isJsonWebToken;
    }

    /**
     * This endpoint requires a role that is mapped to the group1 role
     *
     * @return principal name
     */
    @GET
    @Path("/needsGroup1Mapping")
    @RolesAllowed("Group1MappedRole")
    public String needsGroup1Mapping(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        if (sec.isUserInRole("group1")) {
            return user.getName();
        } else {
            return "User not in role group1";
        }
    }

    /**
     * This endpoint requires a Tester role, and also validates that the caller has the role Echoer by calling
     * {@linkplain SecurityContext#isUserInRole(String)}.
     *
     * @return principal name or FORBIDDEN error
     */
    @GET
    @Path("/checkIsUserInRole")
    @RolesAllowed("Tester")
    public Response checkIsUserInRole(@Context SecurityContext sec) {
        Principal user = sec.getUserPrincipal();
        Response response;
        if (!sec.isUserInRole("Echoer")) {
            response = Response.status(new Response.StatusType() {
                @Override
                public int getStatusCode() {
                    return Response.Status.FORBIDDEN.getStatusCode();
                }

                @Override
                public Response.Status.Family getFamily() {
                    return Response.Status.FORBIDDEN.getFamily();
                }

                @Override
                public String getReasonPhrase() {
                    return "SecurityContext.isUserInRole(Echoer) was false";
                }
            }).build();
        } else {
            response = Response.ok(user.getName(), MediaType.TEXT_PLAIN).build();
        }
        return response;
    }

    @GET
    @Path("/authenticated")
    @Authenticated
    public String checkAuthenticated(@Context SecurityContext sec) {
        if (sec.getUserPrincipal() != null) {
            return sec.getUserPrincipal().getName();
        }
        return "FAILED";
    }

    @GET
    @Path("/getInjectedPrincipal")
    @RolesAllowed("Tester")
    public String getInjectedPrincipal(@Context SecurityContext sec) {
        boolean isJsonWebToken = this.jwtPrincipal instanceof JsonWebToken;
        return "isJsonWebToken:" + isJsonWebToken;
    }

    @POST
    @Path("/postInjectedPrincipal")
    @RolesAllowed("Tester")
    public String postInjectedPrincipal(String body) {
        return body + jwtPrincipal.getName();
    }

    @POST
    @Path("/postInjectedPrincipalJson")
    @RolesAllowed("Tester")
    @Consumes("application/json")
    public String postInjectedPrincipalJson(User user) {
        return "name:" + user.getName() + ",principal:" + jwtPrincipal.getName();
    }

    @GET
    @Path("/heartbeat")
    @PermitAll
    public String heartbeat() {
        return "Heartbeat: " + new Date(System.currentTimeMillis()).toString();
    }
}
