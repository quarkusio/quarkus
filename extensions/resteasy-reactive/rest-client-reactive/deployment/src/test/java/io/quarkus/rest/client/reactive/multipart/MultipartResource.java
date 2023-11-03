package io.quarkus.rest.client.reactive.multipart;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("multipart")
public class MultipartResource {
    public static class Person {
        public String firstName;
        public String lastName;
    }

    @POST
    public void multipart(@RestForm String description,
            @RestForm FileUpload upload,
            @RestForm @PartType(MediaType.APPLICATION_JSON) Person person) {
        // do something
    }
}
