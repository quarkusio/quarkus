package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import java.io.IOException;
import java.nio.file.Files;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

@Path("/multipart")
public class MultipartResource {

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/simple/{times}")
    @NonBlocking
    public String simple(@BeanParam FormData formData, Integer times) {
        if (BlockingOperationSupport.isBlockingAllowed()) {
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
        if (!BlockingOperationSupport.isBlockingAllowed()) {
            throw new RuntimeException("should have dispatched");
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
        if (!BlockingOperationSupport.isBlockingAllowed()) {
            throw new RuntimeException("should have dispatched");
        }
        return formData.status + " - " + formData.getHtmlFiles().size() + " - " + formData.txtFiles.size() + " - "
                + formData.xmlFiles.size();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/optional")
    @NonBlocking
    public String optional(@BeanParam FormData formData) {
        if (BlockingOperationSupport.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return formData.getName() + " - " + formData.active + " - " + formData.getNum() + " - " + formData.getStatus()
                + " - " + (formData.getHtmlPart() != null) + " - " + (formData.xmlPart != null) + " - "
                + (formData.txtFile != null);
    }

}
