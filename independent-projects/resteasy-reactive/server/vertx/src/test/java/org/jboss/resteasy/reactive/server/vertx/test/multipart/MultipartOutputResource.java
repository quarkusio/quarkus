package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import java.io.File;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.core.multipart.MultipartFormDataOutput;

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

    @GET
    @Path("/with-form-data")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultipartFormDataOutput withFormDataOutput() {
        MultipartFormDataOutput form = new MultipartFormDataOutput();
        form.addFormData("name", RESPONSE_NAME, MediaType.TEXT_PLAIN_TYPE);
        form.addFormData("custom-surname", RESPONSE_SURNAME, MediaType.TEXT_PLAIN_TYPE);
        form.addFormData("custom-status", RESPONSE_STATUS, MediaType.TEXT_PLAIN_TYPE)
                .getHeaders().putSingle("extra-header", "extra-value");
        form.addFormData("values", RESPONSE_VALUES, MediaType.TEXT_PLAIN_TYPE);
        form.addFormData("active", RESPONSE_ACTIVE, MediaType.TEXT_PLAIN_TYPE);
        return form;
    }

}
