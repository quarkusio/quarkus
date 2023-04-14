package io.quarkus.observability.example;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "simple")
public interface SimpleClient {
    @GET
    @Path("/api/poke")
    public String poke(@QueryParam("f") int f);
}
