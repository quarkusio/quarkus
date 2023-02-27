package io.quarkus.it.oidc;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.credential.CertificateCredential;

@Path("/service")
public class OidcMtlsEndpoint {

    @Inject
    CertificateCredential cert;

    @Inject
    JsonWebToken accessToken;

    @GET
    @Path("name")
    public String getName() {
        return "Identities: " + cert.getCertificate().getSubjectX500Principal().getName().split(",")[0] + ", "
                + accessToken.getName();
    }
}
