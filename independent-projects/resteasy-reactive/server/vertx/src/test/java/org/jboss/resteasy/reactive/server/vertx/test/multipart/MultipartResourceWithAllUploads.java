package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
