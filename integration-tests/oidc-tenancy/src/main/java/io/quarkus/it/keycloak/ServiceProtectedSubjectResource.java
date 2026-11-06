package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;

@Path("/service/tenant-public-key-subject")
@Authenticated
public class ServiceProtectedSubjectResource {

    @Inject
    JsonWebToken accessToken;

    @GET
    public String getSubject() {
        return "tenant-public-key-subject" + ":" + accessToken.getSubject();
    }
}
