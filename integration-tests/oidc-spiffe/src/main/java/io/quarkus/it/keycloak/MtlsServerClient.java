package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/spiffe/mtls/server")
public interface MtlsServerClient {

    @GET
    String getPrincipal();

}
