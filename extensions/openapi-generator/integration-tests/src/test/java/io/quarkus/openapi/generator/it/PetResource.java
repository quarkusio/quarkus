package io.quarkus.openapi.generator.it;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.openapitools.client.api.PetApi;

@Produces(MediaType.APPLICATION_JSON)
@Path("/petstore")
public class PetResource {

    @RestClient
    @Inject
    PetApi petApi;

    @GET
    @Path("/pet/name/{id}")
    public String getPetName(@PathParam Long id) {
        return petApi.getPetById(id).getName();
    }
}
