package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "spiffe-server")
@Path("/spiffe/mtls/server")
public interface MtlsServerClient {

    @GET
    String getSpiffeId();

    @GET
    @Path("/certificate")
    String getCertificate();

}
