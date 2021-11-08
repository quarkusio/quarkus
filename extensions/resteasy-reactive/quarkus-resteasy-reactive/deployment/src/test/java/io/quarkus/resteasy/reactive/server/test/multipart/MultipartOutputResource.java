package io.quarkus.resteasy.reactive.server.test.multipart;

import java.io.File;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/multipart/output")
public class MultipartOutputResource {

    public static final String RESPONSE_NAME = "a name";
    public static final String RESPONSE_SURNAME = "a surname";
    public static final Status RESPONSE_STATUS = Status.WORKING;
    public static final List<String> RESPONSE_VALUES = List.of("one", "two");
    public static final boolean RESPONSE_ACTIVE = true;

    private final File TXT_FILE = new File("./src/test/resources/lorem.txt");

    @GET
    @Path("/simple")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultipartOutputResponse simple() {
        MultipartOutputResponse response = new MultipartOutputResponse();
        response.setName(RESPONSE_NAME);
        response.setSurname(RESPONSE_SURNAME);
        response.setStatus(RESPONSE_STATUS);
        response.setValues(RESPONSE_VALUES);
        response.active = RESPONSE_ACTIVE;
        return response;
    }

    @GET
    @Path("/string")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public String usingString() {
        return RESPONSE_NAME;
    }

    @GET
    @Path("/with-file")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultipartOutputFileResponse complex() {
        MultipartOutputFileResponse response = new MultipartOutputFileResponse();
        response.name = RESPONSE_NAME;
        response.file = TXT_FILE;
        return response;
    }

}
