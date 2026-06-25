package io.quarkus.it.http3.restclient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("")
public class Resource {

    private final PingClient client;

    public Resource(@RestClient PingClient client) {
        this.client = client;
    }

    @GET
    @Path("/ping")
    public String ping() {
        return "pong";
    }

    @GET
    @Path("/client/ping")
    public String clientPing() {
        return client.ping();
    }
}
