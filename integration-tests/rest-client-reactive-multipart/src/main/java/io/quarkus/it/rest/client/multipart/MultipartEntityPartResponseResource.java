package io.quarkus.it.rest.client.multipart;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/multipart-entity-part-response")
public class MultipartEntityPartResponseResource {

    @GET
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public Response get() throws IOException {
        List<EntityPart> parts = List.of(
                EntityPart.withName("message")
                        .content("from response")
                        .mediaType(MediaType.TEXT_PLAIN_TYPE)
                        .build());
        return Response.ok(parts).build();
    }
}
