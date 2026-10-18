package io.quarkus.it.http3.autotls;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "h3-trust-store")
@Path("/")
public interface HelloClientTrustStore {

    @GET
    @Path("/version")
    String version();
}
