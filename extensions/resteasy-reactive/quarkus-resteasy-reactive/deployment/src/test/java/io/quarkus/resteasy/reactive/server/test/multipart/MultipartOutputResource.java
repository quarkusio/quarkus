package io.quarkus.resteasy.reactive.server.test.multipart;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestResponse;

@Path("/multipart/output")
public class MultipartOutputResource {

    public static final String RESPONSE_NAME = "a name";
    public static final String RESPONSE_SURNAME = "a surname";
    public static final Status RESPONSE_STATUS = Status.WORKING;
    public static final List<String> RESPONSE_VALUES = List.of("one", "two");
    public static final boolean RESPONSE_ACTIVE = true;

    private static final long ONE_GIGA = 1024l * 1024l * 1024l * 1l;
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
    @Path("/rest-response")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public RestResponse<MultipartOutputResponse> restResponse() {
        MultipartOutputResponse response = new MultipartOutputResponse();
        response.setName(RESPONSE_NAME);
        response.setSurname(RESPONSE_SURNAME);
        response.setStatus(RESPONSE_STATUS);
        response.setValues(RESPONSE_VALUES);
        response.active = RESPONSE_ACTIVE;
        return RestResponse.ResponseBuilder.ok(response).header("foo", "bar").build();
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
    public MultipartOutputFileResponse file() {
        MultipartOutputFileResponse response = new MultipartOutputFileResponse();
        response.name = RESPONSE_NAME;
        response.file = TXT_FILE;
        return response;
    }

    @GET
    @Path("/with-large-file")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultipartOutputFileResponse largeFile() throws IOException {
        File largeFile = File.createTempFile("rr-large-file", ".tmp");
        largeFile.deleteOnExit();
        RandomAccessFile f = new RandomAccessFile(largeFile, "rw");
        f.setLength(ONE_GIGA);

        MultipartOutputFileResponse response = new MultipartOutputFileResponse();
        response.name = RESPONSE_NAME;
        response.file = largeFile;
        return response;
    }

    @GET
    @Path("/with-null-fields")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultipartOutputFileResponse nullFields() {
        MultipartOutputFileResponse response = new MultipartOutputFileResponse();
        response.name = null;
        response.file = null;
        return response;
    }

}
