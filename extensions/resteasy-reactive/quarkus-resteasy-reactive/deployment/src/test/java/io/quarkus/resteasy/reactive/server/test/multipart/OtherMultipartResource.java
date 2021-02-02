package io.quarkus.resteasy.reactive.server.test.multipart;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
