package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/client-logger")
@RegisterRestClient(configKey = "w-client-logger")
public interface ClientWithClientLogger {
    @GET
    String call();
}
