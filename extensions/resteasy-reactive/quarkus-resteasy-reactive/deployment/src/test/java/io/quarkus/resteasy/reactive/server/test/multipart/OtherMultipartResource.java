package io.quarkus.resteasy.reactive.server.test.multipart;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.MultipartForm;

@Path("/otherMultipart")
public class OtherMultipartResource {

    @Path("simple")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    public String simple(@MultipartForm OtherFormData formData) {
        return formData.first + " - " + formData.last + " - " + formData.finalField + " - " + OtherFormData.staticField;
    }
}
