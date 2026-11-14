package io.quarkus.oidc.test;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.security.Authenticated;

@Path("/")
public class ApplicationLoginPageResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Context
    UriInfo uriInfo;

    @GET
    @Path("protected")
    @Authenticated
    public String protectedResource() {
        return protectedResponse();
    }

    @GET
    @Path("login")
    @Produces(MediaType.TEXT_HTML)
    public String login(@QueryParam("redirect") String redirect) {
        return loginPage(redirect);
    }

    @GET
    @Path("tenant-restore/protected")
    @Authenticated
    public String tenantRestoreProtected() {
        return protectedResponse();
    }

    @GET
    @Path("tenant-restore/login")
    @Produces(MediaType.TEXT_HTML)
    public String tenantRestoreLogin(@QueryParam("redirect") String redirect) {
        return loginPage(redirect);
    }

    @GET
    @Path("tenant-strict/protected")
    @Authenticated
    public String tenantStrictProtected() {
        return protectedResponse();
    }

    @GET
    @Path("tenant-strict/login")
    @Produces(MediaType.TEXT_HTML)
    public String tenantStrictLogin(@QueryParam("redirect") String redirect) {
        return loginPage(redirect);
    }

    @GET
    @Path("tenant-strict/public")
    public String tenantStrictPublic() {
        return "public";
    }

    @GET
    @Path("tenant-protected-login/protected")
    @Authenticated
    public String tenantProtectedLoginProtected() {
        return protectedResponse();
    }

    @GET
    @Path("tenant-protected-login/login")
    @Authenticated
    @Produces(MediaType.TEXT_HTML)
    public String tenantProtectedLoginLogin(@QueryParam("redirect") String redirect) {
        return loginPage(redirect);
    }

    private String protectedResponse() {
        String query = uriInfo.getRequestUri().getRawQuery();
        return idToken.getName() + ":" + uriInfo.getRequestUri().getRawPath() + (query == null ? "" : "?" + query);
    }

    private static String loginPage(String redirect) {
        if (redirect == null) {
            redirect = "/protected";
        } else if (!redirect.startsWith("/") || redirect.startsWith("//")) {
            throw new BadRequestException();
        }
        return "<html><body><a id=\"login\" href=\"" + redirect.replace("&", "&amp;") + "\">Sign in</a></body></html>";
    }
}
