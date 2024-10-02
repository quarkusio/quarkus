package io.quarkus.it.rest.client.multipart;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.it.rest.client.multipart.model.ContainerDTO;
import io.quarkus.it.rest.client.multipart.model.Dog;
import io.quarkus.it.rest.client.multipart.model.NestedInterface;
import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;
import io.smallrye.common.annotation.NonBlocking;

@Path("/json-serialization")
@NonBlocking
@DisableSecureSerialization
public class JsonSerializationResource {

    @POST
    @Path("/dog-echo")
    @Consumes(MediaType.APPLICATION_JSON)
    public Dog echoDog(Dog dog) {
        return dog;
    }

    @GET
    @Path("/interface")
    public ContainerDTO interfaceTest() {
        return new ContainerDTO(NestedInterface.INSTANCE);
    }
}
