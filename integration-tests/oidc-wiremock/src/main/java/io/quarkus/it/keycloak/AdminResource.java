package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/api/admin")
public class AdminResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    RoutingContext routingContext;

    @Path("bearer")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String admin() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-required-algorithm")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String adminRequiredAlgorithm() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-azure")
    @GET
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public String adminAzure() {
        return "Name:" + identity.getPrincipal().getName() + ",Issuer:" + ((JsonWebToken) identity.getPrincipal()).getIssuer();
    }

    @Path("bearer-no-introspection")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String adminNoIntrospection() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-issuer-resolver/issuer") // don't change the path, avoid default tenant resolver
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String adminIssuerTest() {
        return "static.tenant.id=" + routingContext.get("static.tenant.id");
    }

    @Path("bearer-certificate-full-chain")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String bearerCertificateFullChain() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-chain-custom-validator")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String bearerCertificateCustomValidator() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-certificate-full-chain-root-only")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String bearerCertificateFullChainRootOnly() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-kid-or-chain")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String bearerKidOrChain() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-role-claim-path")
    @GET
    @RolesAllowed("custom")
    @Produces(MediaType.APPLICATION_JSON)
    public String adminCustomRolePath() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-key-without-kid-thumbprint")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String adminNoKidandThumprint() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-wrong-role-path")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String adminWrongRolePath() {
        return "granted:" + identity.getRoles();
    }
}
