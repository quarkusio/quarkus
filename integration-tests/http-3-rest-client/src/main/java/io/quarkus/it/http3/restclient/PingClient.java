package io.quarkus.it.http3.restclient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/ping")
@RegisterRestClient(configKey = "h3-client")
public interface PingClient {

    @GET
    String ping();
}
