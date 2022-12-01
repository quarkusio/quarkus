package io.quarkus.restclient.jaxb.deployment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
