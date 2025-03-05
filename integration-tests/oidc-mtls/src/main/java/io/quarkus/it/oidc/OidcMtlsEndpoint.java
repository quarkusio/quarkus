package io.quarkus.it.oidc;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.Authenticated;
import io.quarkus.security.credential.CertificateCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

@Path("/service")
@Authenticated
public class OidcMtlsEndpoint {

    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken accessToken;

    @Inject
    RoutingContext routingContext;

    @GET
    @Path("mtls-jwt")
    public String getNameJwt() {
        var cred = identity.getCredential(CertificateCredential.class).getCertificate();
        return "Identities: " + cred.getSubjectX500Principal().getName().split(",")[0]
                + ", " + accessToken.getName() + "; "
                + "Client: " + accessToken.getClaim("azp") + "; "
                + "JWT cert thumbprint: " + isJwtTokenThumbprintAvailable() + ", "
                + "introspection cert thumbprint: " + isIntrospectionThumbprintAvailable();
    }

    @GET
    @Path("mtls-introspection")
    public String getNameIntrospection() {
        var cred = identity.getCredential(CertificateCredential.class).getCertificate();
        return "Identities: " + cred.getSubjectX500Principal().getName().split(",")[0] + ", "
                + accessToken.getName() + "; "
                + "Client: " + accessToken.getClaim("azp") + "; "
                + "JWT cert thumbprint: " + isJwtTokenThumbprintAvailable() + ", "
                + "introspection cert thumbprint: " + isIntrospectionThumbprintAvailable();
    }

    @GET
    @Path("mtls-client-with-secret")
    public String getNameMtlsClientWithSecret() {
        var cred = identity.getCredential(CertificateCredential.class).getCertificate();
        return "Identities: " + cred.getSubjectX500Principal().getName().split(",")[0] + ", "
                + accessToken.getName() + "; "
                + "Client: " + accessToken.getClaim("azp") + "; "
                + "JWT cert thumbprint: " + isJwtTokenThumbprintAvailable() + ", "
                + "introspection cert thumbprint: " + isIntrospectionThumbprintAvailable();
    }

    private boolean isJwtTokenThumbprintAvailable() {
        return Boolean.TRUE.equals(routingContext.get(OidcUtils.JWT_THUMBPRINT));
    }

    private boolean isIntrospectionThumbprintAvailable() {

        return Boolean.TRUE.equals(routingContext.get(OidcUtils.INTROSPECTION_THUMBPRINT));
    }
}
