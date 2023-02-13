package io.quarkus.resteasy.reactive.server.test.multipart;

import java.util.List;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.common.annotation.NonBlocking;

@Path("/multipart-all")
public class MultipartResourceWithAllUploads {

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @NonBlocking
    @Path("/simple/{times}")
    public String simple(@BeanParam FormDataWithAllUploads formData, Integer times) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        FileUpload txtFile = formData.getUploads().stream().filter(f -> f.name().equals("txtFile")).findFirst().get();
        return formData.getName() + " - " + formData.active + " - " + times * formData.getNum() + " - " + formData.getStatus()
                + " - " + formData.getUploads().size() + " - " + txtFile.contentType();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @NonBlocking
    @Path("/param/simple/{times}")
    public String simple(
            @RestForm
            // don't set a part type, use the default
            String name,
            @RestForm @PartType(MediaType.TEXT_PLAIN) Status status,
            @RestForm(FileUpload.ALL) List<FileUpload> uploads,
            @RestForm @PartType(MediaType.TEXT_PLAIN) boolean active,
            @RestForm @PartType(MediaType.TEXT_PLAIN) int num,
            Integer times) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        FileUpload txtFile = uploads.stream().filter(f -> f.name().equals("txtFile")).findFirst().get();
        return name + " - " + active + " - " + times * num + " - " + status
                + " - " + uploads.size() + " - " + txtFile.contentType();
    }
}
