package io.quarkus.resteasy.reactive.server.test.multipart;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/file-content-types")
public class FileContentTypesMatchingResource {

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String uploads(@RestForm(FileUpload.ALL) List<FileUpload> uploads) {
        return uploads.stream().map(FileUpload::name).sorted().collect(Collectors.joining(","));
    }
}
