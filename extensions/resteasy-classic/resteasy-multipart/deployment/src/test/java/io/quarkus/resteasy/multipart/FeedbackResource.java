package io.quarkus.resteasy.multipart;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
