package io.quarkus.it.rest.client.multipart;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.it.rest.client.multipart.model.ContainerDTO;
import io.quarkus.it.rest.client.multipart.model.Dog;
import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;
import io.smallrye.common.annotation.NonBlocking;

@Path("/json-serialization")
@NonBlocking
@DisableSecureSerialization
public abstract class AbstractJsonSerializationResource {

    @POST
    @Path("/dog-echo")
    @Consumes(MediaType.APPLICATION_JSON)
    public abstract Dog echoDog(Dog dog);

    @GET
    @Path("/interface")
    public abstract ContainerDTO interfaceTest();

}
