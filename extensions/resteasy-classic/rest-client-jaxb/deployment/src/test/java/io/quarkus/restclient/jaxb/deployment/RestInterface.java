package io.quarkus.restclient.jaxb.deployment;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/hello")
@RegisterClientHeaders
public interface RestInterface {

    @GET
    @Produces(MediaType.APPLICATION_XML)
    Book get();
}
