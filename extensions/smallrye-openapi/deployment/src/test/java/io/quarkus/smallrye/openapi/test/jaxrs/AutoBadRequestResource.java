package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/auto")
public class AutoBadRequestResource {

    @POST
    @Path("/")
    public void addBar(MyBean myBean) {
    }

    @PUT
    @Path("/")
    public void updateBar(MyBean myBean) {
    }

    @POST
    @Path("/string")
    public void addString(String foo) {
    }

    @PUT
    @Path("/string")
    public void updateString(String foo) {
    }

    @POST
    @Path("/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadFile(@RestForm("file") FileUpload file) {
        return Response.accepted().build();
    }

    @PUT
    @Path("/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateFile(@RestForm("file") FileUpload file) {
        return Response.accepted().build();
    }

    @POST
    @Path("/multipart")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadMultipart(@MultipartForm FileUploadForm form) {
        return Response.accepted().build();
    }

    @PUT
    @Path("/multipart")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateMultipart(@MultipartForm FileUploadForm form) {
        return Response.accepted().build();
    }

    @POST
    @Path("/provided")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Successful"),
            @APIResponse(responseCode = "400", description = "Invalid bean supplied")
    })
    public void addProvidedBar(MyBean myBean) {
    }

    @PUT
    @Path("/provided")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Successful"),
            @APIResponse(responseCode = "400", description = "Invalid bean supplied")
    })
    public void updateProvidedBar(MyBean myBean) {
    }

    @POST
    @Path("/nobody")
    public void addNobodyBar() {
    }

    @PUT
    @Path("/nobody")
    public void updateNobodyBar() {
    }

    private static class MyBean {
        public String bar;
    }

    private static class FileUploadForm {
        @RestForm("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] file;

        @RestForm("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName;
    }

}
