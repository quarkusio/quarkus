package io.quarkus.rest.client.reactive.multipart;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

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
