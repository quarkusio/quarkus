package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/resource")
public class OpenApiResourceWithNoTag {

    @GET
    @Path("/auto")
    public String auto() {
        return "by auto tag";
    }

    @POST
    @Path("/auto")
    public String autopost() {
        return "by auto tag";
    }

    @GET
    @Path("/annotated")
    @Tag(name = "From Annotation")
    public String annotated() {
        return "by annotation";
    }

}
