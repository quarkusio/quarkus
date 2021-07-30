package io.quarkus.resteasy.reactive.server.test.multipart;

import java.io.IOException;
import java.nio.file.Files;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

@Path("/multipart")
public class MultipartResource {

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/simple/{times}")
    public String simple(@MultipartForm FormData formData, Integer times) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return formData.getName() + " - " + formData.active + " - " + times * formData.getNum() + " - " + formData.getStatus()
                + " - "
                + formData.getHtmlPart().contentType() + " - " + Files.exists(formData.xmlPart) + " - "
                + formData.txtFile.exists();
    }

    @POST
    @Blocking
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/blocking")
    public Response blocking(@DefaultValue("1") @RestQuery Integer times, FormData formData) throws IOException {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return Response.ok(formData.getName() + " - " + times * formData.getNum() + " - " + formData.getStatus())
                .header("html-size", formData.getHtmlPart().size())
                .header("html-path", formData.getHtmlPart().uploadedFile().toAbsolutePath().toString())
                .header("xml-size", Files.readAllBytes(formData.xmlPart).length)
                .header("xml-path", formData.xmlPart.toAbsolutePath().toString())
                .header("txt-size", Files.readAllBytes(formData.txtFile.toPath()).length)
                .header("txt-path", formData.txtFile.toPath().toAbsolutePath().toString())
                .build();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/same-name")
    public String sameName(FormDataSameFileName formData) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return formData.status + " - " + formData.getHtmlFiles().size() + " - " + formData.txtFiles.size() + " - "
                + formData.xmlFiles.size();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/optional")
    @NonBlocking
    public String optional(@MultipartForm FormData formData) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return formData.getName() + " - " + formData.active + " - " + formData.getNum() + " - " + formData.getStatus()
                + " - " + (formData.getHtmlPart() != null) + " - " + (formData.xmlPart != null) + " - "
                + (formData.txtFile != null);
    }

}
