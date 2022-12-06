package io.quarkus.resteasy.reactive.server.test.multipart;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.quarkus.runtime.BlockingOperationControl;
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
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return formData.getName() + " - " + formData.active + " - " + times * formData.getNum() + " - " + formData.getStatus()
                + " - "
                + formData.getHtmlPart().contentType() + " - " + Files.exists(formData.xmlPart) + " - "
                + formData.txtFile.exists();
    }

    @POST
    @Path("/implicit/simple/{times}")
    @NonBlocking
    public String simpleImplicit(FormData formData, Integer times) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return formData.getName() + " - " + formData.active + " - " + times * formData.getNum() + " - " + formData.getStatus()
                + " - "
                + formData.getHtmlPart().contentType() + " - " + Files.exists(formData.xmlPart) + " - "
                + formData.txtFile.exists();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/param/simple/{times}")
    @NonBlocking
    public String simple(
            // don't set a part type, use the default
            @RestForm String name,
            @RestForm @PartType(MediaType.TEXT_PLAIN) Status status,
            @RestForm("htmlFile") FileUpload htmlPart,
            @RestForm("xmlFile") java.nio.file.Path xmlPart,
            @RestForm File txtFile,
            @RestForm @PartType(MediaType.TEXT_PLAIN) boolean active,
            @RestForm @PartType(MediaType.TEXT_PLAIN) int num,
            Integer times) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return name + " - " + active + " - " + times * num + " - " + status
                + " - "
                + htmlPart.contentType() + " - " + Files.exists(xmlPart) + " - "
                + txtFile.exists();
    }

    @POST
    @Blocking
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/blocking")
    public Response blocking(@DefaultValue("1") @RestQuery Integer times, FormData formData) throws IOException {
        if (!BlockingOperationControl.isBlockingAllowed()) {
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
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should have dispatched");
        }
        return formData.status + " - " + formData.getHtmlFiles().size() + " - " + formData.txtFiles.size() + " - "
                + formData.xmlFiles.size();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/param/same-name")
    public String sameName(@RestForm @PartType(MediaType.TEXT_PLAIN) Status status,
            @RestForm("htmlFile") List<File> htmlFiles,
            @RestForm("txtFile") List<FileUpload> txtFiles,
            @RestForm("xmlFile") List<java.nio.file.Path> xmlFiles) {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should have dispatched");
        }
        return status + " - " + htmlFiles.size() + " - " + txtFiles.size() + " - "
                + xmlFiles.size();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/optional")
    @NonBlocking
    public String optional(FormData formData) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return formData.getName() + " - " + formData.active + " - " + formData.getNum() + " - " + formData.getStatus()
                + " - " + (formData.getHtmlPart() != null) + " - " + (formData.xmlPart != null) + " - "
                + (formData.txtFile != null);
    }

}
