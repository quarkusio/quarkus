package io.quarkus.rest.client.reactive.proxy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "client3")
@Path("/resource")
public interface Client3 {
    @GET
    Response get();
}
