package io.quarkus.resteasy.test.multipart;

import static java.util.stream.Collectors.toList;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

@Path("multipart/")
public class MultipartResource {

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String hello(MultivaluedMap<String, String> formData) {
        return formData.entrySet().stream()
                .map(e -> e.getKey() + ":" + String.join(",", e.getValue()))
                .collect(toList())
                .toString();
    }

}
