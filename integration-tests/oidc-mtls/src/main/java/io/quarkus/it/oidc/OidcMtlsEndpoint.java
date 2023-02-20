package io.quarkus.it.oidc;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.credential.CertificateCredential;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/service")
public class OidcMtlsEndpoint {

    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken accessToken;

    @GET
    @Path("name")
    public String getName() {
        CertificateCredential cert = identity.getCredential(CertificateCredential.class);
        return "Identities: " + cert.getCertificate().getSubjectX500Principal().getName().split(",")[0] + ", "
                + accessToken.getName();
    }
}
