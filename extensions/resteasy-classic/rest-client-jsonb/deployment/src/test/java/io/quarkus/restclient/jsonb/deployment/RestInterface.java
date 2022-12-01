package io.quarkus.restclient.jsonb.deployment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/hello")
@RegisterClientHeaders
public interface RestInterface {

    @GET
    DateDto get();
}
