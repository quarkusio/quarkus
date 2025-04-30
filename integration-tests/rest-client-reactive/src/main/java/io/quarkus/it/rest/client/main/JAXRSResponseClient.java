package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@Path("/client-logger")
@RegisterRestClient(configKey = "w-client-logger")
public interface JAXRSResponseClient {

    @GET
    Response call();

    @GET
    Uni<Response> asyncCall();
}
