package io.quarkus.resteasy.multipart;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

@Path("/test")
public class FeedbackResource {

    @POST
    @Path("/multipart-encoding")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA + ";charset=UTF-8")
    public String postForm(@MultipartForm final FeedbackBody feedback) {
        return feedback.content;
    }
}
