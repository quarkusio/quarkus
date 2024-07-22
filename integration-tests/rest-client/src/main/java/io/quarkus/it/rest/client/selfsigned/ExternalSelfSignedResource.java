package io.quarkus.it.rest.client.selfsigned;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * This has nothing to do with rest-client, but we add it here in order to avoid creating
 * a new integration test that would slow down our CI
 */
@Path("/self-signed")
public class ExternalSelfSignedResource {

    @Inject
    @RestClient
    ExternalSelfSignedClient client;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String perform() throws IOException {
        return String.valueOf(client.invoke().getStatus());
    }

}
