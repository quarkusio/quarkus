package io.quarkus.it.http3.autotls;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "h3-trust-all")
@Path("/")
public interface HelloClientTrustAll {

    @GET
    @Path("/version")
    String version();
}
