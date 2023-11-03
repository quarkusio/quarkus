package io.quarkus.it.rest.client.http2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/ping")
@RegisterRestClient(configKey = "basic-client")
public interface Client {

    @GET
    Response ping();
}
