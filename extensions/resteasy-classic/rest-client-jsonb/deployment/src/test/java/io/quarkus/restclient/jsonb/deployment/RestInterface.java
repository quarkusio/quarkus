package io.quarkus.restclient.jsonb.deployment;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/hello")
@RegisterClientHeaders
public interface RestInterface {

    @GET
    DateDto get();
}
