package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;

import io.smallrye.common.annotation.NonBlocking;

@Path("/multipart-all")
public class MultipartResourceWithAllUploads {

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @NonBlocking
    @Path("/simple/{times}")
    public String simple(@BeanParam FormDataWithAllUploads formData, Integer times) {
        if (BlockingOperationSupport.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        FileUpload txtFile = formData.getUploads().stream().filter(f -> f.name().equals("txtFile")).findFirst().get();
        return formData.getName() + " - " + formData.active + " - " + times * formData.getNum() + " - " + formData.getStatus()
                + " - " + formData.getUploads().size() + " - " + txtFile.contentType();
    }

}
