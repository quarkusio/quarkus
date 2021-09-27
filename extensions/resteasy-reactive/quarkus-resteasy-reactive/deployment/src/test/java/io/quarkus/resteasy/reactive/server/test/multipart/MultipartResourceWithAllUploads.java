package io.quarkus.resteasy.reactive.server.test.multipart;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.MultipartForm;
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
    public String simple(@MultipartForm FormDataWithAllUploads formData, Integer times) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        FileUpload txtFile = formData.getUploads().stream().filter(f -> f.name().equals("txtFile")).findFirst().get();
        return formData.getName() + " - " + formData.active + " - " + times * formData.getNum() + " - " + formData.getStatus()
                + " - " + formData.getUploads().size() + " - " + txtFile.contentType();
    }

}
