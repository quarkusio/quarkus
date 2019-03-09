package io.quarkus.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/")
public class RootResource {

    static class Item {
        String name = "name";

        String getName() {
            return name;
        }
    }

    @GET
    public String root() {
        return "Root Resource";
    }

    @GET
    @Path("/value")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.OBJECT, implementation = Item.class)))
    public Item value() {
        return new Item();
    }

}
