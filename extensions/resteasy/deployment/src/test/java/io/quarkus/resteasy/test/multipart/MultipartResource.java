package io.quarkus.resteasy.test.multipart;

import static java.util.stream.Collectors.toList;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

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